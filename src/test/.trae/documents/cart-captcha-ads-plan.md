# 先锋书店系统增强计划：购物车全局化 + 验证码/人机验证 + 流动广告（借阅系统独立）

> 模式：Plan Mode 产出 · 待用户批准后执行
> 基线：Vue 3 SPA（static/）+ Spring Boot 3.5.3 后端，本会话已完成主前端与品牌导入

## 一、总结（Summary）

为系统补全四块能力，均延续现有「子夜书房 · 极光玻璃」设计语言：

1. **购物车全局化（购买系统）**：顶栏购物车入口（图标 + 数量角标）+ 玻璃抽屉快速预览（改数量/删除/去结算），会员中心保留完整管理；全局响应式 cart 状态让两处实时同步。
2. **借阅系统独立化（与购买彻底分离）**：新增独立 `BorrowFlow` 借阅流程组件与独立弹窗——**不复用、不经过**购物车/结算的任何逻辑与 UI；图书详情「立即借阅」进入专属借阅弹窗（借阅须知 + 期限 30 天说明 + 双重验证）。借阅记录只进入「借阅管理」视图，订单只属于购买系统，两套数据流互不交叉。
3. **验证码 + 人机验证（两套系统均强制）**：
   - 后端新增图形验证码服务（AWT 绘制、session 一次性校验），**强制**应用于登录、注册、购书结算、**借书**四个敏感操作（服务端真校验，防重放）；
   - 前端新增滑块拼图人机验证组件（纯前端、canvas 生成极光底图）；借书流程采用「滑块 → 图形码」双验证，与购物结算（仅图形码）验证强度不同、组件完全独立。
4. **流动广告**：首页轮播 Banner（4 个品牌渐变运营位，自动播放 + 手动切换）+ 顶部无限滚动公告条（marquee），数据为前端运营位配置文件，无需动数据库。

## 二、现状分析（Current State）

- **购物车**：已实现但埋在「会员中心 → 购物车」标签（`js/views/member.js` renderCart/changeQty/removeCart/checkout），无全局入口、无角标、checkout 逻辑绑定在 MemberView 组件内无法复用。后端 `/cart/*`、`/order/checkout` 完整可用。
- **验证**：登录/注册/结算/借阅均无任何人机验证；后端全局无 captcha/banner 相关代码（已 grep 确认）。拦截器 `WebConfig` 的 excludePathPatterns 需放行新验证码接口。
- **广告**：无任何广告/公告组件。
- 关键接口形态：`POST /order/checkout` 接收 `{receiverName, receiverPhone, receiverAddress}`；`POST /borrow/{bookId}` 无 body；`POST /user/login|register` 接收 `{username,password,...}` —— 三者均可无损追加 `captchaCode` 字段。

## 三、变更方案（Proposed Changes）

### A. 购物车全局入口 + 抽屉（购买系统，前端）

| 文件 | 变更 |
|---|---|
| `static/js/store.js` | Store 新增 `cart: reactive({ items: [], loading: false, drawerOpen: false })`；actions 新增 `refreshCart()`（登录则 GET /cart/my，否则清空）；getters 新增 `cartCount`（数量合计）。**cart 状态只服务购买系统，与借阅无关** |
| `static/js/app.js` | ① 顶栏渲染购物车按钮（`ICONS.cart` + 琥珀角标显示 cartCount，仅登录后显示；游客点击打开登录框）；② 新增全局组件 `CartDrawer`：右侧滑入玻璃抽屉（Transition），列表渲染 store.cart.items，内联数量步进器（PUT /cart/{id}?quantity=）、删除（DELETE + confirm）、底部合计与「去结算」；③ 新增全局 `openCheckout()`（从 member.js 抽出）：收货信息表单 + 图形验证码弹窗 → POST /order/checkout（带 captchaCode）→ 下单成功弹窗（保留现有支付入口），成功后 `refreshCart()` |
| `static/js/views/member.js` | 购物车 tab 改为消费 `store.cart`（删除本地 cart 状态），数量/删除操作调用 store 方法后 `refreshCart()`；结算改调全局 `openCheckout()`；登录态变化时由 store watch 自动刷新 |
| `static/css/style.css` | 新增：`.cart-fab`（顶栏按钮+角标）、`.drawer-overlay/.drawer`（右侧抽屉 + 弹簧滑入动画）、结算弹窗扩展样式 |

### B. 借阅系统独立化 + 双重验证（前端，与购买系统零耦合）

借阅是独立业务线：入口在图书详情弹窗，流程走专属 `BorrowFlow` 弹窗组件；借阅成功后数据只出现在「借阅管理 → 我的书架」。**不产生订单、不进购物车、不与 CartDrawer/openCheckout 共享任何状态或组件。**

| 文件 | 变更 |
|---|---|
| `static/js/app.js` | 新增全局组件 `BorrowFlow`（props: book，独立玻璃弹窗，与结算弹窗完全分离）：① 借阅须知区（借期 30 天、可续借 2 次、逾期 0.5 元/天、当日库存展示）；② 第一步滑块拼图人机验证（内嵌 JigsawSlider）；③ 第二步图形验证码输入（内嵌 CaptchaInput，独立请求，与结算验证码互不影响）；④ 「确认借阅」→ POST /borrow/{bookId}（带 captchaCode）→ 成功态（应还日期回显）→ 引导前往「借阅管理」查看 |
| `static/js/views/home.js` | 图书详情弹窗「立即借阅」按钮改为打开 `BorrowFlow`（原直接 POST 逻辑删除）；详情弹窗内保留「加入购物车」（购买线） |
| `static/js/views/borrow.js` | 我的书架空态文案增加「去书架借一本」CTA（跳检索视图）；其余不动 |
| `static/js/ui.js` | 无结构变更 |
| `static/css/style.css` | 新增：`.borrow-flow`（分步指示器 + 步骤切换动画）、验证通过态样式；与 `.drawer/.modal` 样式类命名隔离，避免视觉/逻辑混淆 |

### C. 图形验证码（后端强制校验）+ 滑块人机验证

后端（4 新增/修改文件）：

| 文件 | 变更 |
|---|---|
| 新增 `service/CaptchaService.java` | 生成 4 位码（剔除 0O1lI 易混字符）→ AWT Graphics2D 绘制 120×44 PNG（随机旋转字符 + 3 条干扰弧线 + 噪点），存 session 属性 `CAPTCHA_CODE` + `CAPTCHA_EXPIRE`（5 分钟）；`verifyAndConsume(session, code)`：存在/未过期/忽略大小写匹配 → 销毁并返回 true（一次性防重放）。校验失败抛 `BusinessException("验证码错误或已过期")` |
| 新增 `controller/CaptchaController.java` | `GET /captcha/image`：写 PNG 字节流（`image/png`，Cache-Control: no-store）；`POST /captcha/verify`：返回 `{valid}` 供前端预检（可选用） |
| 修改 `controller/UserController.java` | `login`/`register` 开头调用 `captchaService.verifyAndConsume(session, body.get("captchaCode"))` |
| 修改 `controller/OrderController.java` | `checkout` 同上校验（购买系统） |
| 修改 `controller/BorrowController.java` | `borrow` 同上校验（借阅系统，独立生效） |
| 修改 `config/WebConfig.java` | excludePathPatterns 追加 `/captcha/image`（游客可获取） |

前端公共验证组件（供两套系统各自独立实例化，不共享状态）：

| 文件 | 变更 |
|---|---|
| `static/js/app.js` | 新增两个全局组件：① `CaptchaInput`（props: modelValue）——玻璃输入框 + 120×44 验证码图（`/api/captcha/image?t=` 时间戳防缓存，点击刷新，加载态），后端 400 报「验证码错误」后自动刷新；② `JigsawSlider`（props: onSuccess 回调）——纯前端滑块拼图：canvas 程序化生成 280×140 极光渐变底图 + 随机缺口与拼图块（每次随机），拖动对齐（容差 4px）带轨迹高亮、成功打勾动画，失败 300ms 回弹 |
| `static/js/views/member.js` / `app.js` AuthModal | 登录/注册表单追加 CaptchaInput，提交体带 `captchaCode` |
| `static/css/style.css` | 新增：`.captcha-row`、`.captcha-img`、`.slider-wrap/.slider-track/.slider-block/.slider-handle`、成功/失败态动画 |

### D. 流动广告：轮播 Banner + 滚动公告条

| 文件 | 变更 |
|---|---|
| 新增 `static/js/ads.js` | 运营位数据 `AD_DATA`：`banners` 4 条（tag 徽标 / 大标题 / 副标 / CTA 文案与跳转 `{view, keyword|categoryId}` / 各异的品牌渐变：琥珀→紫、青→蓝、朱红→紫、金→绿）；`notices` 3-4 条公告文本。纯前端配置，改文案无需动后端 |
| `static/js/app.js` | 注册 `PromoBanner`（hero 与分类 chips 之间）：5s 自动轮播 + 指示器 + 左右箭头 + 触摸滑动，玻璃卡片内渐变背景、tag/标题/副标/CTA，点击执行 `actions.go(view)` 或带关键词跳检索；`NoticeTicker`：置顶于 home 视图顶部（或顶栏下方全局），喇叭图标 + 无缝 marquee（内容双份拼接 translateX 关键帧），hover 暂停；`prefers-reduced-motion` 时停用自动轮播 |
| `static/js/views/home.js` | 模板插入 `<promo-banner>` 与 `<notice-ticker>` |
| `static/index.html` | `<script src="js/ads.js">`（在 store.js 之后、views 之前） |
| `static/css/style.css` | 新增：`.promo-banner`（渐变卡 + 轮播切换动画）、`.ticker`（marquee 无缝滚动 + 呼吸光点）、指示器/箭头样式 |

### 交付与联调约定

- 所有新前端文件改完 **必须同步 Copy-Item 到 `../../../../target/classes/static`**（运行实例从磁盘实时读取该目录）；后端 Java 改动需重启应用。
- 验证码图片依赖 session cookie，同源 `/api` 直接可用，无 CORS 问题。

## 四、假设与决策（已与用户确认）

1. 购物车采用「全局入口 + 抽屉预览」，不做独立页面；仅服务购买系统。
2. **借阅与购买是两条独立业务线**（用户明确要求）：借阅不经过购物车/结算，拥有专属 BorrowFlow 弹窗与「滑块 + 图形码」双验证；购买结算使用单图形码验证。两套系统不共享业务状态与弹窗组件（仅复用无状态的 CaptchaInput/JigsawSlider 基础件）。
3. 验证方案：后端图形码（真安全，绑定 session 一次性消费）强制用于登录/注册/结算/借阅；滑块拼图为纯前端人机验证，叠加于借书流程；支付步骤不加码（订单流程内，避免过度摩擦）。
4. 广告为前端运营位配置（`js/ads.js`），不建后台广告表；文案更新只需改该文件。
5. 短信验证码不可行（离线环境），不做。
6. AWT 生成图片在 Windows 本地运行无 headless 问题。

## 五、验证步骤（Verification）

1. `mvn compile` 通过 → 重启 `mvn spring-boot:run`（同步编译资源）。
2. 后端单点验证（浏览器/PowerShell）：
   - `GET /api/captcha/image` 返回 PNG 且每次不同；错误码登录/结算/借阅均返回「验证码错误或已过期」；正确码操作成功且旧码不可复用（一次性）。
3. 浏览器全流程实测（agent-browser，桌面 1440 + 移动 390）：
   - **购买线**：详情加购 → 顶栏角标更新 → 抽屉改数量/删除 → 去结算（图形码）→ 下单成功 → 角标清零；全程不出现借阅逻辑；
   - **借阅线**：详情「立即借阅」→ 独立借阅弹窗 → 滑块拼图 → 图形码 → 借阅成功 →「借阅管理」我的书架出现记录；全程不产生订单、不触碰购物车；
   - 登录（错码→提示刷新→对码成功）、注册；
   - 首页：轮播自动切换/箭头/指示器/CTA 跳转、公告条无缝滚动 hover 暂停；
   - 会员中心购物车与抽屉数据一致；
   - 移动端：抽屉全宽适配、滑块可拖动、轮播可滑动、无横向溢出；
   - 全程控制台零报错。
4. Fidelity 检查：新组件全部使用玻璃设计语言（blur/细边框/琥珀点缀），借阅弹窗与结算弹窗视觉风格统一但内容/标题/图标可明确区分。
