# 生产就绪收尾：缺陷修复与加固方案

## Context

项目主体功能（前台门户 / 会员中心 / 后台管理台 / 购买与借阅双线）已交付，69 个测试用例全绿，生产配置与容器化已完成。

在「生产就绪」验收前的两轮只读排查中，发现了一批**真实缺陷**（非风格问题）：包括一个会让分页功能崩溃的前端 `return` 遗漏、购物车金额显示为 ¥0.00 的 SQL 契约错误、订单状态机缺环导致的账务不一致、RBAC 权限表从未被调用、以及借阅读接口内嵌全表 UPDATE 等。本方案按「风险从低到高、每批可独立验证」的顺序修复这些问题，最终交付一个通过全量测试与端到端验收的生产版本。

**已确认的四项决策**：
1. RBAC → 落地权限表 + fail-open 兜底
2. 订单状态机 → 完整修复（新增确认收货 + 收紧取消 + 放宽退款）
3. 支付 → 保持模拟实现 + 现有接入文档
4. 数据库凭据 → dev 保留默认值，prod 强制环境变量

---

## 批次 1：后端低风险修复（各自独立，可一次提交）

单个文件的局部改动，互不耦合。

| 项 | 文件 | 改动 |
|----|------|------|
| B1【中高】 | [BookService.java](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/java/com/xianfeng/bookstore/service/BookService.java#L70-L83) | `addBook` 补 `@Transactional(rollbackFor = Exception.class)`（两处 insert：`bookMapper` + `stockMapper`，失败会留下"有书无库存"脏数据） |
| B13【低】 | 同上 :50 | `book.getStatus() == 0` → `Integer.valueOf(0).equals(book.getStatus())`，避免 Integer null 拆箱 NPE |
| B14【低】 | CommentService / StockService | `audit` 校验 `auditStatus ∈ {1,2}`；`setWarn` 校验 `warnQuantity >= 0`；`stockIn` 校验 `bookId` 存在 |
| B5【中】 | [BorrowService.java](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/java/com/xianfeng/bookstore/service/BorrowService.java#L97-L110) | `renew` 补 `@Transactional`；新增「已逾期不可续借」校验（`now.isAfter(dueTime)` → 拒绝）；新到期日从 `max(now, dueTime)` 起算（原实现在旧值上加 30 天，逾期状态下续借后仍逾期）；不再强制 `status=0` 抹掉逾期标记 |
| B11【低】 | IndexController :32 | `index()` 返回值包成 `R.ok(...)`，回归统一响应契约 |
| B12【低】 | `common/RestControllerAdvice.java` | 删除（与 Spring 同名注解冲突的空自定义注解，全项目零引用） |
| B15【低】 | [application.yml](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/resources/application.yml) :56 | 删除 `mapper-locations`（`classpath:mapper/*.xml` 目录不存在，所有 Mapper 均为注解实现） |
| B16【低】 | application-dev.yml :14 | 删除 `pool.max-size`（无 `@ConfigurationProperties` 绑定，是无效配置项） |

**验证**：`mvn test` → 69 绿（`BorrowServiceTest` 8 例不应受影响）。

> B5 注意：`renew` 现有 12 个用例中如含「逾期后仍可续借」的断言，需同步调整为期望抛 `BusinessException` —— 这是**修正错误语义**而非削弱断言。

---

## 批次 2：借阅逾期读写分离（必须同一提交）

**问题**：`myBorrows` / `adminList` / `overdueList` 三个 GET 方法内都调用 `refreshOverdue()`，执行一次全表 `UPDATE t_borrow SET status=2 WHERE status=0 AND due_time < now()`。读接口带写操作会破坏只读语义（无法走只读事务/从库）、每次翻页产生锁竞争与 binlog、且无事务导致中间态可见。

**方案**：定时任务 + 判定下推 SQL。

1. 启动类加 `@EnableScheduling`
2. [BorrowService.java](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/java/com/xianfeng/bookstore/service/BorrowService.java#L135-L142) 的 `refreshOverdue` 整体搬入新方法，改为 `@Scheduled(fixedDelay = 600_000, initialDelay = 60_000)` + `@Transactional`
3. 三个读方法删除 `refreshOverdue()` 调用
4. **关键配套**：`BorrowMapper.selectOverdue` 的 `where b.status = 2` 改为 `where b.status <> 1 and b.due_time < now()`

第 4 步不可省略 —— 否则定时任务未跑时逾期列表会永远为空，看起来像功能回归。

**为何不用「写路径顺带刷新」**：还书时的罚款本就按 `dueTime` 独立计算、不依赖 `status`；前端 `borrow.js` 的 `barCls`/`leftText` 也按 `dueTime` 计算。因此逾期标记延迟最多 10 分钟无任何业务损失。

**验证**：`mvn test` + 手工把某条记录 `due_time` 置为过去且 `status=0`，确认 `/borrow/admin/borrow/overdue` 仍返回该条（`BorrowFlowIntegrationTest` 只断言 HTTP 200 与 `$.code`，不受影响）。

---

## 批次 3：订单状态机补全（分两步递进）

现状：状态 3「已完成」全项目无写入点；`cancel` 允许取消已支付订单（只回补库存、不处理资金 → 钱货两空）；`requestRefund` 仅允许已发货订单，已支付未发货无法退款。

**状态码不变**（0待付款 1待发货 2已发货 3已完成 4已取消 5退款中 6已退款），**不加新列**（表无物流字段，避免 DDL 迁移）。

### 3a：先加能力（验证通过后再收紧）

- 新增 `PUT /order/receive/{orderNo}`：仅 `2 → 3`
- `requestRefund` 放宽为 `1 或 2 → 5`（进入 5 后不再满足前置条件，天然拦截重复申请）

### 3b：再收紧取消

- `cancel` 由「0 或 1」收紧为**仅 0 → 4**，从根上消除资金不一致
- 前端 [member.js](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/resources/static/js/views/member.js) 同步：取消按钮条件改 `orderStatus === 0`；退款按钮改 `orderStatus === 1 || orderStatus === 2`；新增 `orderStatus === 2` 的「确认收货」按钮

**互斥闭环**：`3` 状态下 `refund`（前置只认 1/2）与 `receive`（前置只认 2）均不可达，形成硬互斥。

**如何保证不破坏现有 3 个 `OrderCheckoutIntegrationTest`**：这 3 例只走 `POST /order/checkout`（成功 / 错误验证码 / 未登录），完全不触碰 cancel/refund/receive。**严禁在本次改动中顺手修改 `checkout`** 的校验顺序、`R` 包装、验证码错误消息与拦截器对 `/order/**` 的 401 行为。

**分两步的理由**：若先收紧 `cancel` 而未同步放宽 `refund`，已支付订单会陷入「既不能取消也不能退款」的死单状态。

---

## 批次 4：RBAC 权限表落地（必须同一提交）

**问题**：`t_role_permission` 表与 `RoleMapper.selectPermUrlsByRoleId` 从未被调用，拦截器只硬编码 roleId 3/4，与 SQL 中按权限细分的初始数据不符。

**关键发现**：`t_permission` 的 seed 中 **perm_id=1 与 8 的 `perm_url` 与实际路径不符** ——
- `1 图书管理`: `/admin/book` → 实际是 `/book/admin/*`
- `8 借阅管理`: `/admin/borrow` → 实际是 `/borrow/admin/borrow/*`
- 其余 2~7 的前缀均正确

**改动**：

1. 修正 seed（[bookstore.sql](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/sql/bookstore.sql#L202-L210) 直接改；另在 `indexes.sql` 末尾追加幂等 UPDATE 供已有库使用）
2. [AuthInterceptor.java](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/java/com/xianfeng/bookstore/config/interceptor/AuthInterceptor.java) 改 `@Component` + 构造注入 `RoleMapper`；`WebConfig` 改为注入该 Bean 注册（当前是 `new AuthInterceptor()`，无法注入）
3. **判定顺序**：`/admin/super/**`（roleId=4 硬编码，保留）→ roleId 3/4 白名单门槛（保留，401/403 语义与 `SecurityIntegrationTest` 完全一致）→ **叠加** perm_url 前缀匹配；匹配失败时 **fail-open（放行 + `log.warn`）**
4. **性能**：权限数据静态，用 `ConcurrentHashMap` 懒加载缓存（`computeIfAbsent` + `filter(Objects::nonNull)`），每请求零查库。不引入 Spring Cache（未配 CacheManager，成本大于收益）；不做 TTL（无权限维护界面）

**fail-open 是「不锁死店员」的保险** —— 即使 perm_url 配置有误，店员也不会被 403 挡住整个后台。

**覆盖核对（已逐条比对，role 3 拥有 1~8 全部权限）**：`/user/admin/list`、`/book/admin/page`、`/category/admin/*`、`/admin/stock/*`、`/order/admin/page`、`/comment/admin/page`、`/admin/stats/overview`、`/borrow/admin/borrow/list` 全部命中。因此 `SecurityIntegrationTest.adminList_staff_returns200` 保持绿。

**新增测试**：补一个集成测试逐条断言 role 3 可访问上述 8 条后台路径（防止未来回归时把店员锁死）。

---

## 批次 5：评价归属校验（可独立）

**问题**：`CommentService.add` 无购买/借阅归属校验、无重复评价约束 → 任意登录用户可对任意图书刷评。

**方案**：复用**已注入的 `CommentMapper`**（不新增字段，保持 `CommentServiceTest` 的 `@InjectMocks` 结构不变），新增三个查询：

```java
@Select("select count(*) from t_order_item i join t_order o on i.order_id=o.order_id " +
        "where o.user_id=#{userId} and i.book_id=#{bookId} and o.order_status in (1,2,3)")
int countPaidPurchase(...);

@Select("select count(*) from t_borrow where user_id=#{userId} and book_id=#{bookId}")
int countBorrowed(...);

@Select("select count(*) from t_comment where user_id=#{userId} and book_id=#{bookId} and audit_status <> 2")
int countActiveComment(...);
```

`add` 中在「图书存在性校验」之后插入：先查重复（`countActiveComment > 0` → 拒绝），再查归属（两个 count 均为 0 → 拒绝）。只认已支付及以后状态（1/2/3），排除未付款与已取消。

**测试影响（如实说明）**：5 个用例中 4 个在更早的校验点抛错，不受影响；仅 `add_success_defaultsAuditStatusToZero_andTrimsContent` 会因 mock 默认返回 0 而失败 → 该用例加一行 `when(commentMapper.countPaidPurchase(USER_ID, BOOK_ID)).thenReturn(1);`。这是**必要的测试适配**，不削弱任何断言。

**前端**：不做按钮预判（需额外接口，属过度设计）；未购买用户仍可见「写评价」，点击后由 toast 提示被拒。同时修 F8 —— 后台评价审核表把 `#{{c.bookId}}` 改为显示书名。

---

## 批次 6：购物车唯一约束与并发兜底（必须同一提交）

**问题**：`t_cart` 缺 `uk_user_book` 唯一约束，且索引全在独立的 `indexes.sql` 里 → 只执行 `bookstore.sql` 的新库无约束，而 `CartService.add` 是「先查后插」无事务无锁，并发加购会产生重复行。

**方案**：

1. `bookstore.sql` 的 `t_cart` DDL 增加 `UNIQUE KEY uk_user_book (user_id, book_id)`（注意 `PRIMARY KEY (cart_id)` 行末补逗号）
2. 已有库走 `indexes.sql` —— 它已有的逻辑是「临时表去重（数量求和）→ `add_index_if_absent`」，存储过程按 `information_schema.STATISTICS` 判断存在性，**可重复执行、先清脏数据再建索引**。同步修正两处文件的注释使其自洽
3. `CartService.add` 补 `@Transactional`，`insert` 捕获 `DuplicateKeyException` 后回退为「重查 + 累加」（并发时用户不再看到 500；该异常只回滚单条语句，且此处是最后一步写，安全）

**禁止**在文档里让人手工 `ALTER TABLE` —— 有脏数据时必然失败。

**验证**：并发加购脚本 + `indexes.sql` 连跑两次确认幂等。

---

## 批次 7：前端缺陷修复（可拆多次提交）

| 项 | 文件 | 改动 |
|----|------|------|
| **F1【高】** | [home.js](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/resources/static/js/views/home.js#L273-L274) :273 | **`return` 对象补 `load`**。模板 :61/:63 的分页按钮 `@click="page--; load()"` 引用了它，但 return 中没有 → 图书超过 12 本时点分页抛 `TypeError: _ctx.load is not a function`，检索翻页功能不可用。**必须修** |
| **F2【高】** | [CartMapper.java](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/java/com/xianfeng/bookstore/mapper/CartMapper.java) :17 | `b.discount_price as price` → `COALESCE(b.discount_price, b.price) as price`。无折扣图书 `discount_price` 为 NULL → 前端购物车合计显示 ¥0.00 |
| F3【中】 | home.js :263-271 | Banner CTA 的 `Store.pendingSearch` 只在 `onMounted` 消费一次；用户已在首页时点击 `jump.view==='home'` 的 Banner 不会重挂载 → 检索条件被静默丢弃。改为 `watch` 消费 |
| F4【中】 | member.js :190 | 删除 `loadCart()` 死分支（全文件未定义，当前不可达） |
| F5【中】 | borrow.js :151 + BorrowService.adminList | 后端 `adminList` 补返回 `total`；前端补分页控件（现固定 `page=1&size=10`，第 11 条起记录不可见） |
| F6【中】 | member.js :135 | 我的订单补分页控件（现固定 `size=8`） |
| F7【中】 | home.js :61/:63、admin.js :218/:220、home.js :41 | 分页纯图标按钮补 `aria-label`；图书卡片 `<article @click>` 补 `role`/`tabindex`/键盘事件（键盘与读屏当前不可达） |

---

## 批次 8：配置、文档与最终验收

1. **B9**：[application.yml](file:///d:/2026next%20Spring%20Boot/xianfeng-bookstore/src/main/resources/application.yml) 改为 `${DB_PASSWORD:Root666.}` 形式（保住「克隆即可跑」），`application-prod.yml` 保持无默认值的 `${DB_PASSWORD}` 强制注入；README 标注「默认值仅供开发，生产必须注入环境变量」
2. **README 更新**：新增订单状态机流转表、RBAC 权限表说明、逾期标记机制、新增的分页能力；更新测试用例数
3. **`../../../../docs/payment-integration.md`**：保持现状（支付维持模拟实现）

---

## 风险最高点与防御

| 风险点 | 后果 | 防御措施 |
|--------|------|----------|
| **RBAC（最高）** | perm 匹配写错或 seed 未同步 → 店员被 403 锁死整个后台 | 保留 roleId 3/4 门槛作为第一道闸；perm 匹配 fail-open + warn 日志；补集成测试逐条断言 8 条后台路径 |
| **订单前后端不同步** | 先删 `cancel@1` 会让已支付订单变成死单 | 严格按 3a → 3b 两步走，每步单独验证后再推进 |
| **逾期读写分离** | 只删写不改 SQL → 逾期列表永远为空，像功能回归 | `selectOverdue` 判定下推与删除 `refreshOverdue` 必须同 commit，并手工造数据 curl 验证 |
| **去明文密码** | 去掉默认值后本地启动直接失败 | 采用已确认方案：dev 保留默认值，仅 prod 强制注入 |

---

## 验证方式

### 每批次
- `mvn test`：基线 69 绿 → 批次 4/5 后应增至 70+ 绿
- 涉及接口的批次用 `curl` 直连 `http://localhost:8080/api` 验证（服务已在 8080 运行）

### 最终全量验收
1. `mvn clean package` → BUILD SUCCESS，测试全绿，产出 `../../../../target/xianfeng-bookstore-1.0.0.jar`
2. `java -jar` 启动打包制品，确认 `GET /actuator/health` 返回 `{"status":"UP"}`（注意 prod profile 下需注入 `DB_PASSWORD`，dev 下直启）
3. 浏览器 E2E（`http://localhost:8080/api/`）：
   - **购买线**：登录 → 加购 → 抽屉校验金额（F2 修复点）→ 结算 → 下单 → 支付 → 确认收货 → 订单显示「已完成」
   - **借阅线**：详情 → 立即借阅 → 滑块 → 图形码 → 借出成功 → 借阅台可见
   - **评价线**：未购买用户评价被拒 → 购买后评价成功 → 后台审核通过 → 前台可见（批次 5）
   - **后台线**：用 `staff/123456` 逐个打开 5 个 tab（验证 RBAC 未锁死店员）
   - **分页线**：造 13+ 本图书，前台点第 2 页（验证 F1 修复点）
   - **账号**：`admin/123456`（店长）、`staff/123456`（店员）
4. 移动端断点（390px）回归：顶栏、抽屉、弹窗无溢出

### 交付
所有批次验证通过后创建 commit（遵循仓库现有提交风格）。