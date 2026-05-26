# 变更记录

## 2026-05-25

### Sprint 1 — HomeScreen 重构
- Agent 发送问候 (ChatMessage, 非页面组件)
- 渐变压缩为薄条 (仅状态栏区域)
- 菜单/电话/通知图标仅主页
- 删除所有副标题
- ChatViewModel.sendDailyGreeting()
- 用户气泡浅蓝白 #E3F0FD, 最大280dp
- AgentHintBadge 浅粉胶囊
- ProductCard: 到手价+原价划线+来源标签+右箭头
- ChatInputBar: 相机+文本+语音+发送

### Sprint 2 — CompareScreen 重建
- 搜索栏: 相机+placeholder+红色按钮
- 分类标签: M3默认指示器(选中变大+蓝色)
- 统一2列商品网格(删除双卡区+更多列表)
- 挂画式价格跟踪面板(无下滑条)
- 价格趋势卡片+简易趋势条

### 渐变条统一
- 所有页面从0px开始, 高度=statusBar×0.75
- HomeScreen: 含图标(33dp)
- Compare/Explore/Profile: 空白无内容
- 图标大小: 20dp→22dp→30dp→33dp (多次调整)

### 文件变更
- 修改: HomeScreen.kt ×5, ChatViewModel.kt, MessageBubble.kt, ProductCard.kt ×2, GradientScreenBackground.kt ×4, CompareScreen.kt ×5, NavGraph.kt ×2, ExploreScreen.kt, ProfileScreen.kt, MainBottomNavBar.kt, Color.kt
- 删除: ChatGuideScreen.kt (合并入HomeScreen)
- 文档: DESIGN.md v1→v5, DEV_PLAN.md, CHANGELOG.md
