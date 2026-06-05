package com.shopping.agent.data.mock

import com.shopping.agent.data.model.*

/**
 * 探索页社交分享贴 Mock 数据 — 50 条
 */
object MockExplorePosts {

    val posts: List<ExplorePost> = listOf(
        // ── 运动鞋 (8) ──
        ExplorePost(postId = "post_001",
            author = PostAuthor("auth_001", "跑鞋控小王", "auth1"),
            product = PostProduct("shoe_001", "运动鞋", "Nike Air Max 270 日常穿搭分享", listOf(
                "https://picsum.photos/seed/a95aaf5/400/600",
                "https://picsum.photos/seed/6611e86a/400/600",
            )),
            content = PostContent("这双鞋已经陪我走了三个月了",
                listOf("刚拿到手的时候觉得颜值在线，黑白配色真的很百搭。穿了三个月下来，鞋底的270气垫缓震确实有效果，每天通勤走一万步脚底也不累。",
                       "透气性方面夏天穿也不闷脚，网面材质很薄但包裹感不差。唯一的小遗憾是白色鞋边容易脏，需要经常擦。",
                       "搭配的话我觉得配牛仔裤和运动裤都好看，比想象中更日常。如果有朋友在犹豫要不要入手，我的建议是作为第一双气垫跑鞋非常值得。")
            )),
        ExplorePost(postId = "post_002",
            author = PostAuthor("auth_002", "晨跑日记", "auth2"),
            product = PostProduct("shoe_002", "运动鞋", "Adidas Ultraboost 23 三个月深度体验", listOf(
                "https://picsum.photos/seed/19e1d9e/400/600",
                "https://picsum.photos/seed/4889a99b/400/600",
            )),
            content = PostContent("用三个月告诉你Ultraboost值不值得买",
                listOf("先说结论：值。全掌Boost中底的脚感真的上头，踩下去像踩在棉花糖上但又不会陷进去，回弹很及时。",
                       "我每周跑步三次，每次五公里左右，膝盖和脚踝比之前穿普通跑鞋的时候舒服多了。马牌橡胶大底抓地力也好，下雨天跑步不滑。",
                       "日常穿也很舒服，袜套式设计穿脱方便。如果预算在千元以内想要顶级缓震，这双闭眼入。")
            )),
        ExplorePost(postId = "post_003",
            author = PostAuthor("auth_003", "球鞋收藏家", "auth3"),
            product = PostProduct("shoe_003", "运动鞋", "李宁飞电3.0 Elite — 国货竞速新选择", listOf(
                "https://picsum.photos/seed/63f1dab1/400/600",
                "https://picsum.photos/seed/14728dad/400/600",
            )),
            content = PostContent("用飞电跑了人生第一个半马",
                listOf("种草飞电好久了，赶上双十一终于入手。第一次穿就跑了15公里，全掌碳板的推进感非常明显，配速直接提了15秒。",
                       "䨻丝鞋面透气性无敌，跑完全程袜子还是干的。鞋底的GCU止滑大底在湿滑路面也抓得很稳。",
                       "作为国货旗舰竞速鞋，这个表现已经完全不输进口品牌了。推荐给想突破自己PB的跑友。")
            )),
        ExplorePost(postId = "post_004",
            author = PostAuthor("auth_004", "学生党穿搭", "auth4"),
            product = PostProduct("shoe_004", "运动鞋", "安踏C37 4.0 — 学生党的平价好鞋", listOf(
                "https://picsum.photos/seed/388d8141/400/600",
                "https://picsum.photos/seed/679794a5/400/600",
            )),
            content = PostContent("不到500块就能拥有的踩云感",
                listOf("作为学生党预算有限，安踏C37真的救了我。中底软硬刚刚好，上课走路去图书馆一整天脚不累。",
                       "颜值也OK的，白色款很清爽。穿了两个月鞋面也没有变形，质量对得起这个价格。",
                       "如果你和我一样每天走路1万步以上又不想花大价钱，C37绝对是性价比首选。")
            )),
        ExplorePost(postId = "post_005",
            author = PostAuthor("auth_005", "复古穿搭阿威", "auth5"),
            product = PostProduct("shoe_005", "运动鞋", "New Balance 574 元祖灰 — 日系穿搭必备", listOf(
                "https://picsum.photos/seed/33d40063/400/600",
                "https://picsum.photos/seed/6ea29f8c/400/600",
            )),
            content = PostContent("一双可以穿十年的复古跑鞋",
                listOf("574的经典不用多说了，元祖灰配色不管什么时候拿出来穿都不过时。翻毛皮的质感是其他材质比不了的。",
                       "脚感偏硬但日常走路足够了，主要是百搭！工装裤、牛仔裤、阔腿裤都能配，我已经攒了三双不同配色。",
                       "日系穿搭的精髓就是一双574，随便一拍都出片。送给喜欢复古风的你。")
            )),
        ExplorePost(postId = "post_006",
            author = PostAuthor("auth_006", "潮流前线", "auth6"),
            product = PostProduct("shoe_006", "运动鞋", "Yeezy Slide 拖鞋 — 夏天最强单品", listOf(
                "https://picsum.photos/seed/10bf3d0f/400/600",
                "https://picsum.photos/seed/553d393c/400/600",
            )),
            content = PostContent("这个夏天我穿了整整两个月",
                listOf("入手之前觉得一双拖鞋卖这个价有点离谱，但穿上的瞬间就真香了。EVA发泡材质的脚感太软糯了。",
                       "洗澡穿、出门穿、倒垃圾穿，整个夏天就没脱下来过。耐脏好打理，湿了也不变形。",
                       "唯一的缺点就是假货太多了，记得认准正规渠道。这双鞋会成为你夏天使用频率最高的一双。")
            )),
        ExplorePost(postId = "post_007",
            author = PostAuthor("auth_007", "户外达人老刘", "auth7"),
            product = PostProduct("shoe_007", "运动鞋", "HOKA ONE ONE 飞速羚羊5 — 越野跑者的福音", listOf(
                "https://picsum.photos/seed/7e535e18/400/600",
                "https://picsum.photos/seed/502ea9d1/400/600",
            )),
            content = PostContent("穿上它跑山真的是一种享受",
                listOf("作为一个重度越野跑爱好者，HOKA Speedgoat 5 是我穿过最好的越野鞋。Vibram大底在湿滑的石头路面上抓地力超强。",
                       "厚底缓震在碎石路上像在跑平路，下山的时候膝盖完全不会痛。鞋面防水性也不错，过小溪不湿脚。",
                       "如果你喜欢跑山但担心伤膝盖，闭眼入HOKA，包你满意。")
            )),
        ExplorePost(postId = "post_008",
            author = PostAuthor("auth_008", "马拉松菜鸟", "auth8"),
            product = PostProduct("shoe_008", "运动鞋", "Saucony 胜利21 — 大体重跑者的缓震救星", listOf(
                "https://picsum.photos/seed/6d15f7d6/400/600",
                "https://picsum.photos/seed/313410d7/400/600",
            )),
            content = PostContent("180斤大体重跑者亲测",
                listOf("作为一个180斤的跑者，之前穿别的跑鞋跑完膝盖疼三天。换成胜利21之后这个问题完全消失了。",
                       "PWRRUN+中底的缓震深度太足了，每一步都能感觉到鞋在帮你吸收冲击。鞋楦也够宽，我的宽脚终于不用受罪了。",
                       "大体重跑者选鞋最重要的就是缓震和保护，胜利21在这两点上做到了极致。强烈推荐！")
            )),

        // ── 数码 (7) ──
        ExplorePost(postId = "post_009",
            author = PostAuthor("auth_009", "数码极客小陈", "auth9"),
            product = PostProduct("dg_001", "数码", "AirPods Pro 2 — 降噪效果到底怎么样", listOf(
                "https://picsum.photos/seed/1db1a887/400/600",
                "https://picsum.photos/seed/717fa02/400/600",
            )),
            content = PostContent("深度使用半年后的真实感受",
                listOf("AirPods Pro 2 的降噪是我用过最自然的。它不是那种把所有声音都闷住的降噪，而是更像把世界音量调低了。",
                       "透明模式很神奇，戴着跟没戴一样。通勤神器，地铁上开启降噪瞬间安静。续航也OK，配合充电盒一周充一次就够了。",
                       "唯一吐槽的是白色太容易脏了，建议买AppleCare+。整体来说千元价位TWS的最佳选择之一。")
            )),
        ExplorePost(postId = "post_010",
            author = PostAuthor("auth_010", "智能家居控", "auth10"),
            product = PostProduct("dg_002", "数码", "小米手环8 Pro — 用了三个月的真实体验", listOf(
                "https://picsum.photos/seed/3797b881/400/600",
                "https://picsum.photos/seed/13bfe8d9/400/600",
            )),
            content = PostContent("399元到底值不值",
                listOf("先说优点：屏幕大、续航长、运动模式多。1.74寸AMOLED屏幕看通知完全够用，阳光下也清晰。",
                       "100多种运动模式覆盖了我所有需求，跑步游泳骑行都能自动识别。NFC刷公交门禁太方便了，出门现在只带手环。",
                       "槽点：表带舒适度一般，建议换第三方表带。跟手机APP的同步偶尔延迟。但399的价格这些都可以忍。")
            )),
        ExplorePost(postId = "post_011",
            author = PostAuthor("auth_011", "手游鉴赏家", "auth11"),
            product = PostProduct("dg_003", "数码", "ROG Phone 8 Pro — 游戏手机的天花板", listOf(
                "https://picsum.photos/seed/3d1c6197/400/600",
                "https://picsum.photos/seed/21277ab3/400/600",
            )),
            content = PostContent("可能是最强安卓游戏手机",
                listOf("165Hz刷新率的屏幕玩原神和崩坏真的丝滑到哭。散热做得很好，连续游戏一小时也只是温热。AirTrigger肩键打FPS简直外挂。",
                       "6000mAh大电池带着出差一天不用充电。外观RGB灯效也很拉风，在地铁上掏出来绝对是全场焦点。",
                       "缺点就是有点重，220g+，而且拍照一般。但作为游戏手机它已经做到极致了。")
            )),
        ExplorePost(postId = "post_012",
            author = PostAuthor("auth_012", "摄影爱好者", "auth12"),
            product = PostProduct("dg_004", "数码", "索尼 A7C II — 轻便全画幅的最佳选择", listOf(
                "https://picsum.photos/seed/5a57ca6c/400/600",
                "https://picsum.photos/seed/671f54c1/400/600",
            )),
            content = PostContent("带它去了三个国家后的深度评测",
                listOf("作为一个旅行摄影爱好者，A7C II 完美满足了我对轻便和画质的双重追求。机身只有514g，配一个35mm定焦可以装进随身小包里。",
                       "3300万像素的全画幅传感器，拍风景细节丰富，肤色还原也很自然。AI对焦人眼和鸟眼都很准，旅行中抓拍成功率极高。",
                       "唯一的遗憾是单卡槽和菜单还是有点复杂。但如果你和我一样喜欢轻装出行又不想牺牲画质，这机器绝对值得。")
            )),
        ExplorePost(postId = "post_013",
            author = PostAuthor("auth_013", "效率工具控", "auth13"),
            product = PostProduct("dg_005", "数码", "iPad Pro M4 — 能替代电脑吗", listOf(
                "https://picsum.photos/seed/4a68ef80/400/600",
                "https://picsum.photos/seed/6c2577da/400/600",
            )),
            content = PostContent("用iPad Pro工作一个月的真实体验",
                listOf("先说结论：可以替代80%的电脑工作。我用它写稿、做PPT、剪简单的视频都完全OK。M4芯片的性能过剩，多任务切换毫不卡顿。",
                       "妙控键盘的打字手感意外地好，屏幕的mini-LED看HDR内容是一种享受。续航也够一天高强度使用。",
                       "但Excel复杂操作和代码开发还是回到MacBook上更顺手。所以iPad Pro是很好的第二设备，但还不能完全替代。")
            )),
        ExplorePost(postId = "post_014",
            author = PostAuthor("auth_014", "HiFi烧友", "auth14"),
            product = PostProduct("dg_006", "数码", "森海塞尔 IE 600 — 退烧级入耳", listOf(
                "https://picsum.photos/seed/79b11132/400/600",
                "https://picsum.photos/seed/218fb53e/400/600",
            )),
            content = PostContent("一副可以听十年的耳塞",
                listOf("森海IE600的调音太高级了。不是那种轰头的低音，而是精准、中性、自然的声音。听古典乐弦乐的泛音细节一览无余。",
                       "ZR01非晶态锆合金外壳，硬度仅次于钻石，根本不怕划伤。佩戴也很舒服，可以连续戴几个小时不累。",
                       "唯一的门槛就是价格和需要好前端。但如果你是追求极致音质的烧友，这可能是你烧到最后的那一副塞子。")
            )),
        ExplorePost(postId = "post_015",
            author = PostAuthor("auth_015", "极简主义者", "auth15"),
            product = PostProduct("dg_007", "数码", "Kindle Scribe — 读书和笔记的新方式", listOf(
                "https://picsum.photos/seed/1887b9c6/400/600",
                "https://picsum.photos/seed/56d44feb/400/600",
            )),
            content = PostContent("用Kindle Scribe读了一百本书",
                listOf("大屏幕看PDF太舒服了，终于不用缩放来缩放去了。手写笔做笔记的延迟很低，跟在纸上写差不多。",
                       "续航超级长，充一次电可以用好几周。现在读书的时候可以随手标注感想，再也不用另外拿笔记本了。",
                       "缺点是系统还是偏封闭，导出笔记不够方便。但对于只想安静读书和做笔记的人来说，这台设备足够了。")
            )),

        // ── 服装 (5) ──
        ExplorePost(postId = "post_016",
            author = PostAuthor("auth_016", "夏日穿搭日记", "auth16"),
            product = PostProduct("cl_001", "服装", "优衣库 UT 哆啦A梦联名 — 夏天第一件T恤", listOf(
                "https://picsum.photos/seed/c39d382/400/600",
                "https://picsum.photos/seed/1633af2b/400/600",
            )),
            content = PostContent("79元就能拥有的快乐",
                listOf("优衣库UT系列每年都会买几件，今年的哆啦A梦联名太可爱了！纯棉面料贴身很舒服，洗了几次也没有变形。",
                       "宽松版型对微胖星人很友好，配牛仔短裤和帆布鞋就是满分夏日穿搭。79元的价格冲就完了。",
                       "推荐给和我一样喜欢卡通联名又追求舒适的朋友，这件T恤会成为你夏天穿着频率最高的一件。")
            )),
        ExplorePost(postId = "post_017",
            author = PostAuthor("auth_017", "法式女孩的日常", "auth17"),
            product = PostProduct("cl_002", "服装", "ZARA 碎花连衣裙 — 海岛度假穿搭分享", listOf(
                "https://picsum.photos/seed/5267c52e/400/600",
                "https://picsum.photos/seed/658f40d0/400/600",
            )),
            content = PostContent("穿上它去三亚拍了超多好看的照片",
                listOf("一直想找一条拍照好看又日常的碎花裙，ZARA这条完全符合我的预期。雪纺面料轻盈飘逸，海风吹起来裙摆飘动特别出片。",
                       "V领设计显瘦又修饰脸型，腰间的系带可以自由调节松紧。去海边度假穿它拍照发朋友圈收获了一大波赞。",
                       "唯一的提醒是浅色碎花要注意防走光，建议穿安全裤。但整体来说349元买到这个质感真的很满意了。")
            )),
        ExplorePost(postId = "post_018",
            author = PostAuthor("auth_018", "职场穿搭指南", "auth18"),
            product = PostProduct("cl_003", "服装", "海澜之家免烫衬衫 — 职场新人必备", listOf(
                "https://picsum.photos/seed/333f1dbc/400/600",
                "https://picsum.photos/seed/8338b42/400/600",
            )),
            content = PostContent("一件不用熨的衬衫有多幸福",
                listOf("入职第一天就穿它去的，免烫面料真的很省心。扔洗衣机洗完了晾干直接穿，完全没有褶皱！",
                       "修身版型很精神，搭配西裤和皮鞋就是标准的通勤穿搭。透气性也不错，夏天在空调房里刚刚好。",
                       "269元的价格在商务衬衫里算性价比很高的了，推荐给和我一样刚入职场的打工人。")
            )),
        ExplorePost(postId = "post_019",
            author = PostAuthor("auth_019", "防晒达人", "auth19"),
            product = PostProduct("cl_004", "服装", "蕉下冰薄防晒衣 — 夏天出门的神器", listOf(
                "https://picsum.photos/seed/3987f9b2/400/600",
                "https://picsum.photos/seed/5de9db16/400/600",
            )),
            content = PostContent("UPF50+的防晒衣到底有没有用",
                listOf("亲测有用！穿着它在户外走了一下午，手臂完全没有泛红的迹象。冰丝面料的触感很凉爽，不会闷热。",
                       "轻薄到可以塞进随身小包，出门带着完全不占地方。连帽设计还能保护脖子和脸，全方位防晒。",
                       "如果你和我一样怕晒黑又不想涂油腻的防晒霜，这件蕉下冰薄防晒衣就是你的夏天必须装备。")
            )),
        ExplorePost(postId = "post_020",
            author = PostAuthor("auth_020", "羽绒服选购指南", "auth20"),
            product = PostProduct("cl_005", "服装", "波司登轻薄羽绒服 — 冬天不再臃肿", listOf(
                "https://picsum.photos/seed/10aecb0d/400/600",
                "https://picsum.photos/seed/1952bb43/400/600",
            )),
            content = PostContent("899元买到可以收纳成小包的羽绒服",
                listOf("去年冬天买了这件波司登轻薄羽绒服，真的是我买过最值的冬季单品。90%白鸭绒填充，保暖不输厚款但重量只有一半。",
                       "最惊喜的是它可以折叠收纳成一个小包，出差带着完全不占行李箱空间。防泼水面料下小雨也不用怕。",
                       "黑色款百搭所有冬装，穿去上班、逛街、户外徒步都很合适。这个价格买到这种品质的羽绒服，值了。")
            )),

        // ── 箱包 (5) ──
        ExplorePost(postId = "post_021",
            author = PostAuthor("auth_021", "出差达人老张", "auth21"),
            product = PostProduct("bg_001", "箱包", "新秀丽 20寸登机箱 — 出差党的省心神器", listOf(
                "https://picsum.photos/seed/19e85a1b/400/600",
                "https://picsum.photos/seed/3d612bd9/400/600",
            )),
            content = PostContent("带着它飞了十几趟的真实体验",
                listOf("作为每周都要出差的人，一个好登机箱太重要了。新秀丽这个铝框箱用了快一年，轮子依然顺滑安静。",
                       "20寸刚好可以带上飞机不用托运，节约了很多时间。内部分区设计合理，三天出差的东西都能装下。TSA海关锁也让人安心。",
                       "唯一的缺点是铝框款比普通款重一点，但对于需要坚固行李箱的人来说，这额外的重量是值得的。")
            )),
        ExplorePost(postId = "post_022",
            author = PostAuthor("auth_022", "通勤好物分享", "auth22"),
            product = PostProduct("bg_002", "箱包", "Herschel Little America — 颜值和容量都在线的双肩包", listOf(
                "https://picsum.photos/seed/6d8e9875/400/600",
                "https://picsum.photos/seed/306acb24/400/600",
            )),
            content = PostContent("背了一年的通勤包",
                listOf("买它的原因很简单：好看。复古翻盖设计和磁吸扣真的很戳我。用了一年发现不止好看，也很实用。",
                       "能装15寸笔记本和各种杂物，出门一天完全够用。肩带填充够厚，背久了也不会肩膀痛。",
                       "如果你也想要一个既有颜值又实用的双肩包，Herschel绝对不会让你失望。")
            )),
        ExplorePost(postId = "post_023",
            author = PostAuthor("auth_023", "包包控少女", "auth23"),
            product = PostProduct("bg_003", "箱包", "Coach 经典C纹托特包 — 我的第一个轻奢包", listOf(
                "https://picsum.photos/seed/def8f0e/400/600",
                "https://picsum.photos/seed/5997c599/400/600",
            )),
            content = PostContent("用了一年后的真实使用报告",
                listOf("作为自己的第一个轻奢包，Coach这款托特真的没有让我失望。经典Signature C纹帆布耐磨又不显旧。",
                       "容量超级大，轻松放下笔记本电脑、水杯、化妆包和雨伞。通勤逛街都很实用，可手提可肩背两用设计很灵活。",
                       "虽然价格不便宜，但综合品质和使用频率来看性价比还是可以的。推荐给需要大容量通勤包的职场女性。")
            )),
        ExplorePost(postId = "post_024",
            author = PostAuthor("auth_024", "户外徒步日记", "auth24"),
            product = PostProduct("bg_004", "箱包", "迪卡侬 MH100 徒步背包 — 入门徒步的首选", listOf(
                "https://picsum.photos/seed/48c27e49/400/600",
                "https://picsum.photos/seed/3211e591/400/600",
            )),
            content = PostContent("200块就能买到的专业徒步背包",
                listOf("刚开始接触徒步的时候买的第一个背包，用了一年多还是很满意。30L容量刚好适合一日徒步，装下水和干粮完全够。",
                       "透气背负系统真的很给力，夏天走一天也不会后背湿透。防水面料在小雨中保护了我的电子设备。",
                       "作为入门者的第一件装备，199元的价格无可挑剔。如果你刚开始尝试户外徒步，这个包是你的最佳起点。")
            )),
        ExplorePost(postId = "post_025",
            author = PostAuthor("auth_025", "日系穿搭控", "auth25"),
            product = PostProduct("bg_005", "箱包", "MUJI 帆布托特包 — 58块的快乐", listOf(
                "https://picsum.photos/seed/256195c4/400/600",
                "https://picsum.photos/seed/4ca6435c/400/600",
            )),
            content = PostContent("我买了三个不同颜色",
                listOf("58块一个，买了原色、黑色、藏青三种颜色换着背。帆布材质越用越软越有质感，完全可以当购物袋和书包两用。",
                       "容量装下几本书、便当和钱包绰绰有余。内袋设计也很贴心，小物件可以单独收纳不怕找不到。",
                       "极简的设计风格配什么都好看，是我使用频率最高的包包。强烈推荐给喜欢简约风格的你。")
            )),

        // ── 美妆 (5) ──
        ExplorePost(postId = "post_026",
            author = PostAuthor("auth_026", "护肤小白的进阶路", "auth26"),
            product = PostProduct("be_001", "美妆", "兰蔻小黑瓶精华 — 30天真实测评", listOf(
                "https://picsum.photos/seed/4723bdfa/400/600",
                "https://picsum.photos/seed/c75d4d0/400/600",
            )),
            content = PostContent("坚持用了30天后皮肤的变化",
                listOf("一开始是因为换季皮肤状态很糟才入手的。蛋清质地很清爽，油皮使用也不闷痘。用了大概一周就能感觉到皮肤变细腻了。",
                       "一个月后最明显的变化是肤色均匀了，以前脸颊泛红的情况改善了很多。后续上面霜吸收也更快了。",
                       "虽然价格不便宜，但一瓶可以用三四个月，算下来性价比还可以。推荐给想改善肤质但又怕闷痘的姐妹。")
            )),
        ExplorePost(postId = "post_027",
            author = PostAuthor("auth_027", "美妆控Kitty", "auth27"),
            product = PostProduct("be_002", "美妆", "SK-II神仙水 — 一年使用心得", listOf(
                "https://picsum.photos/seed/6b281027/400/600",
                "https://picsum.photos/seed/3aa427ea/400/600",
            )),
            content = PostContent("用了整整一年的真实反馈",
                listOf("神仙水真的是我用过最神奇的护肤品。一年前我T区出油很严重毛孔也比较粗大，现在已经改善了一大半。",
                       "坚持早晚使用，大概三个月的时候就能明显感觉到皮肤变透亮了。上妆也更加服帖，粉底液都不需要买太贵的了。",
                       "唯一的槽点是味道不太好闻，但效果摆在那里我忍了。如果你还在犹豫要不要入手，我的建议是：值得。")
            )),
        ExplorePost(postId = "post_028",
            author = PostAuthor("auth_028", "学生党美妆", "auth28"),
            product = PostProduct("be_003", "美妆", "花西子空气蜜粉 — 国货定妆神器", listOf(
                "https://picsum.photos/seed/27b4e206/400/600",
                "https://picsum.photos/seed/4be925ad/400/600",
            )),
            content = PostContent("129元的国货定妆也太能打了吧",
                listOf("之前一直用大牌散粉，朋友推荐花西子的时候半信半疑。结果第一次上脸就被惊艳到了——粉质细腻得像空气！",
                       "控油效果在夏天可以撑6-8小时，中午补一次就够了。自带的天鹅绒粉扑和镜子也很方便外出携带。",
                       "129元的价格买到的品质完全不输三四百的进口品牌。如果你在找平价好用的定妆粉，花西子不会让你踩雷。")
            )),
        ExplorePost(postId = "post_029",
            author = PostAuthor("auth_029", "成分党护肤", "auth29"),
            product = PostProduct("be_004", "美妆", "珀莱雅双抗精华 — 抗氧抗糖一瓶搞定", listOf(
                "https://picsum.photos/seed/627bdffb/400/600",
                "https://picsum.photos/seed/1ec3064e/400/600",
            )),
            content = PostContent("成分党告诉你这瓶精华为什么好用",
                listOf("作为一个天天研究护肤品成分的人，珀莱雅双抗的配方真的做得很良心。虾青素+麦角硫因的组合白天抗氧化效果明显。",
                       "用了两个月，熬夜后的黄气明显减轻了。质地轻盈不黏腻，油皮在夏天用也毫无压力。早晚都用，一瓶可以用两个月。",
                       "239元的价格买到这个成分配置真心划算。如果你开始担心初老迹象但又不想花大价钱，双抗是个很好的入门选择。")
            )),
        ExplorePost(postId = "post_030",
            author = PostAuthor("auth_030", "防晒狂魔", "auth30"),
            product = PostProduct("be_005", "美妆", "安耐晒小金瓶 — 海岛度假防晒实测", listOf(
                "https://picsum.photos/seed/1d7ededf/400/600",
                "https://picsum.photos/seed/43c2cf5e/400/600",
            )),
            content = PostContent("在泰国暴晒一周没有黑",
                listOf("去泰国度假之前专门买了安耐晒小金瓶，一周下来每天在沙滩上暴晒，回来之后同事们都说我完全没有晒黑！",
                       "成膜很快轻薄不油腻，防水防汗效果确实厉害。Aqua Booster遇水更强的技术不是吹的，游泳后还能保持防护。",
                       "去海边度假防晒只推荐它。虽然价格比超市防晒贵一些，但效果和肤感完全值回票价。夏天囤几瓶不会后悔。")
            )),

        // ── 食品 (5) ──
        ExplorePost(postId = "post_031",
            author = PostAuthor("auth_031", "零食收割机", "auth31"),
            product = PostProduct("fd_001", "食品", "三只松鼠坚果大礼包 — 年货送礼首选", listOf(
                "https://picsum.photos/seed/45658a68/400/600",
                "https://picsum.photos/seed/672709b4/400/600",
            )),
            content = PostContent("过年送这个被亲戚夸了",
                listOf("去年过年买了三盒三只松鼠坚果礼盒送给亲戚，被夸会选礼物。礼盒包装喜庆大气，里面的坚果品质也很高。",
                       "夏威夷果和巴旦木很酥脆，腰果的烘焙程度刚刚好。独立小包装设计很贴心，不用担心开封后变潮。",
                       "138元的价格送礼有面子又不贵。今年过年果断又囤了几盒，强烈推荐给大家做年货伴手礼。")
            )),
        ExplorePost(postId = "post_032",
            author = PostAuthor("auth_032", "夜宵研究协会", "auth32"),
            product = PostProduct("fd_002", "食品", "良品铺子猪肉脯 — 我的追剧必备零食", listOf(
                "https://picsum.photos/seed/6997b283/400/600",
                "https://picsum.photos/seed/3b5c55e/400/600",
            )),
            content = PostContent("追剧的时候能吃三袋",
                listOf("良品铺子的猪肉脯真的上瘾，蜜汁和香辣两种口味都好吃。蜜汁款甜咸适中很有嚼劲，香辣款微微辣很提神。",
                       "肉质紧实不是那种面粉感很重的劣质肉脯。独立小包装设计很方便，办公室抽屉里放几袋随时补充能量。",
                       "29.9元两袋性价比无敌，比超市买的那种散装肉脯品质好太多了。无限回购中！")
            )),
        ExplorePost(postId = "post_033",
            author = PostAuthor("auth_033", "巧克力品鉴师", "auth33"),
            product = PostProduct("fd_003", "食品", "德芙巧克力礼盒 — 送女友的甜蜜选择", listOf(
                "https://picsum.photos/seed/3e38c9a4/400/600",
                "https://picsum.photos/seed/76de1ffd/400/600",
            )),
            content = PostContent("情人节送这个被夸很会",
                listOf("情人节送了女友这盒德芙混合装，她开心了一整天。牛奶巧克力丝滑浓郁，黑巧克力不会太甜刚刚好。",
                       "每颗独立包装干净卫生，放在包里随时可以吃一颗。礼盒的设计也很精美，完全不用再另外包装。",
                       "39.9元的价格就能让女朋友开心，这笔投资太划算了！推荐给所有不知道该送什么礼物的男同胞们。")
            )),
        ExplorePost(postId = "post_034",
            author = PostAuthor("auth_034", "咖啡日记", "auth34"),
            product = PostProduct("fd_004", "食品", "星巴克咖啡豆 — 在家也能做到的精品咖啡", listOf(
                "https://picsum.photos/seed/4e7440f4/400/600",
                "https://picsum.photos/seed/775f9b89/400/600",
            )),
            content = PostContent("在家手冲星巴克的快乐",
                listOf("疫情期间买了手冲壶和星巴克咖啡豆，从此开启了自己的咖啡师之路。中度烘焙的豆子风味均衡，坚果和巧克力的香气很迷人。",
                       "每天早上花五分钟磨豆手冲，整个厨房都是咖啡香。比去店里买划算太多了，一袋豆子可以冲三十杯。",
                       "如果你也想在家享受精品咖啡的乐趣，从这袋豆子开始不会错。配上法压壶或手冲壶，品质不输咖啡店。")
            )),
        ExplorePost(postId = "post_035",
            author = PostAuthor("auth_035", "牛奶爱好者", "auth35"),
            product = PostProduct("fd_005", "食品", "蒙牛特仑苏 — 我的早餐标配", listOf(
                "https://picsum.photos/seed/1c4b1a21/400/600",
                "https://picsum.photos/seed/306045a0/400/600",
            )),
            content = PostContent("每天早上必喝的纯牛奶",
                listOf("特仑苏喝了好几年了，奶香浓郁口感醇厚。每100ml含3.6g优质乳蛋白，作为早餐搭配面包麦片刚刚好。",
                       "家里囤了两箱，爸妈都说比超市散装的好喝。利乐钻包装常温保存也方便，不用占冰箱空间。",
                       "79元12盒性价比可以，如果你和我一样想喝好一点的牛奶但不想花太多钱，特仑苏是很好的选择。")
            )),

        // ── 家居 (5) ──
        ExplorePost(postId = "post_036",
            author = PostAuthor("auth_036", "懒人居家好物", "auth36"),
            product = PostProduct("hm_001", "家居", "美的空气炸锅 — 厨房小白的救星", listOf(
                "https://picsum.photos/seed/132a9032/400/600",
                "https://picsum.photos/seed/2bd94d29/400/600",
            )),
            content = PostContent("买了之后几乎每天都在用",
                listOf("作为一个只会煮方便面的厨房小白，美的空气炸锅真的救了我。炸薯条、烤鸡翅、烘焙小蛋糕，什么都试了一遍全成功了。",
                       "免翻面设计太省心了，扔进去等十几分钟就能吃。5L容量刚好够两个人用，清洗也很方便不粘涂层一擦就干净。",
                       "399块钱就能实现炸鸡自由，比去外面吃划算多了还更健康。强烈推荐给和我一样厨艺有限但又想吃好的朋友。")
            )),
        ExplorePost(postId = "post_037",
            author = PostAuthor("auth_037", "家居美学控", "auth37"),
            product = PostProduct("hm_002", "家居", "戴森V15吸尘器 — 值得这么贵吗", listOf(
                "https://picsum.photos/seed/1701ba98/400/600",
                "https://picsum.photos/seed/398cd599/400/600",
            )),
            content = PostContent("花五千块买吸尘器是不是疯了",
                listOf("买了一年了，可以负责任地说：值。激光探测微尘的功能一开始觉得是噱头，用了才知道有多好用——肉眼看不到的灰尘全都显现出来了。",
                       "吸力真的很强，床垫上的螨虫和皮屑清理得很彻底。转换各种吸头可以清洁沙发、窗帘、汽车座椅，一机多用。",
                       "如果你的预算允许，这是一个可以用很多年的好工具。清洁效果真的比几百块的吸尘器好太多了。")
            )),
        ExplorePost(postId = "post_038",
            author = PostAuthor("auth_038", "国货家居爱好者", "auth38"),
            product = PostProduct("hm_003", "家居", "富安娜60支长绒棉四件套 — 睡眠升级", listOf(
                "https://picsum.photos/seed/404a6db2/400/600",
                "https://picsum.photos/seed/1aeae624/400/600",
            )),
            content = PostContent("换了这套床品睡眠质量明显提升",
                listOf("之前一直用普通的纯棉四件套，换了富安娜这个60支长绒棉之后才意识到好的床品有多重要。贡缎工艺的面料丝滑到像睡在五星级酒店。",
                       "新疆长绒棉的透气性很好，夏天也不闷热。活性印染洗了好几次也没有褪色，品质确实杠杠的。",
                       "499元一套的价格不算便宜但物有所值。如果你也想提升睡眠质量，从好床品开始是个不错的方向。")
            )),
        ExplorePost(postId = "post_039",
            author = PostAuthor("auth_039", "养猫星人的日常", "auth39"),
            product = PostProduct("pt_001", "宠物", "小佩自动猫砂盆 — 养猫人的解放神器", listOf(
                "https://picsum.photos/seed/1ef94161/400/600",
                "https://picsum.photos/seed/200f4702/400/600",
            )),
            content = PostContent("出差一周回家猫砂盆还是干净的",
                listOf("养了两只猫每天铲屎真的心累，入手小佩自动猫砂盆后感觉生活质量提升了不止一个档次。猫咪上完厕所自动旋转铲屎，集便仓一周装两次就够了。",
                       "APP还能记录猫咪上厕所的次数和时长，意外发现有只猫上厕所频率异常，及时去看医生避免了更严重的问题。",
                       "虽然1299元不便宜，但用上之后你一定会后悔没有早买。如果你也是养猫人，这可能是你最值得的一笔投资。")
            )),
        ExplorePost(postId = "post_040",
            author = PostAuthor("auth_040", "萌宠日记", "auth40"),
            product = PostProduct("pt_002", "宠物", "皇家成猫粮 — 我家猫咪吃了一年长胖了", listOf(
                "https://picsum.photos/seed/7a3e6d7/400/600",
                "https://picsum.photos/seed/3502c79c/400/600",
            )),
            content = PostContent("从流浪猫到胖橘只用了一年",
                listOf("去年收养了一只流浪猫，瘦得皮包骨头。朋友推荐了皇家F32成猫粮，吃了一年现在已经是圆滚滚的小胖橘了。",
                       "精准营养配方真的有效，毛发光泽度比之前好了很多。消化吸收也好，便便很成型不臭。性价比来说4kg一大袋可以吃两个月。如果你家猫主子挑食或者偏瘦，可以试试皇家。")
            )),

        // ── 图书 (5) ──
        ExplorePost(postId = "post_041",
            author = PostAuthor("auth_041", "科幻迷小林", "auth41"),
            product = PostProduct("bk_001", "图书", "三体全集 — 改变我世界观的一套书", listOf(
                "https://picsum.photos/seed/8c5e83e/400/600",
                "https://picsum.photos/seed/1cba7a9d/400/600",
            )),
            content = PostContent("看完之后仰望星空的感觉都不一样了",
                listOf("花了两周时间啃完了三体三部曲，全程高能。黑暗森林法则太震撼了，很长一段时间看到夜空都有一种莫名的敬畏感。",
                       "刘慈欣的硬科幻功底真的厉害，很多物理概念讲得很清楚，就算没有理工背景也能看懂。水滴战役和降维打击的篇章读得我头皮发麻。",
                       "不管你是不是科幻迷，这套书都应该读。它不仅是一部科幻小说，更是一种关于文明与存在的思考。")
            )),
        ExplorePost(postId = "post_042",
            author = PostAuthor("auth_042", "睡前读书人", "auth42"),
            product = PostProduct("bk_002", "图书", "活着 — 读完哭了三次的书", listOf(
                "https://picsum.photos/seed/24bf83bc/400/600",
                "https://picsum.photos/seed/1871a5b1/400/600",
            )),
            content = PostContent("这是一本用最简单的语言写最沉重人生的书",
                listOf("福贵这一生太苦了，余华用极其克制的白描写出了最深沉的悲悯。读到有庆去世那一段真的忍不住哭了。",
                       "这不仅仅是一个人的故事，它是一代中国人的命运缩影。读完之后才真正理解了什么叫\"活着本身就是活着的意义\"。",
                       "如果你还没有读过《活着》，尽快读。它不会给你答案，但会让你对生命有不一样的感受。")
            )),
        ExplorePost(postId = "post_043",
            author = PostAuthor("auth_043", "读书笔记博主", "auth43"),
            product = PostProduct("bk_003", "图书", "人类简史 — 颠覆认知的一本书", listOf(
                "https://picsum.photos/seed/3565da8a/400/600",
                "https://picsum.photos/seed/4eb63912/400/600",
            )),
            content = PostContent("这本书让我重新思考什么是人",
                listOf("赫拉利的宏大叙事能力太强了。七万年的智人历史被他讲得像一部跌宕起伏的小说。认知革命、农业革命、科学革命三条主线清晰有力。",
                       "最让我反思的是\"虚构的故事\"这个概念——国家、货币、法律、人权，本质上都是人类共同相信的故事。它让我对很多习以为常的事物有了全新的认识。如果你今年只想读一本书，就读这本《人类简史》吧。")
            )),
        ExplorePost(postId = "post_044",
            author = PostAuthor("auth_044", "心理学爱好者", "auth44"),
            product = PostProduct("bk_004", "图书", "被讨厌的勇气 — 阿德勒哲学入门", listOf(
                "https://picsum.photos/seed/beebe2b/400/600",
                "https://picsum.photos/seed/59a92f26/400/600",
            )),
            content = PostContent("读完才知道自己一直在自寻烦恼",
                listOf("这本书以对话体展开，一位青年和一位哲人的深夜对话，深入浅出地解释了阿德勒心理学的核心思想。",
                       "\"课题分离\"这个概念对我帮助最大——不是所有的烦恼都是你该负责的，别人的评价和期待是别人的课题。这个观点让我从很多人际关系焦虑中解脱出来。如果你经常被他人评价所困扰，这本书会给你答案。")
            )),
        ExplorePost(postId = "post_045",
            author = PostAuthor("auth_045", "推理小说迷", "auth45"),
            product = PostProduct("bk_005", "图书", "白夜行 — 东野圭吾的巅峰之作", listOf(
                "https://picsum.photos/seed/22cb31b5/400/600",
                "https://picsum.photos/seed/25d3baa7/400/600",
            )),
            content = PostContent("看完之后一整晚没睡着",
                listOf("《白夜行》不是传统意义上的推理小说，它更像一部跨越十九年的社会派悲歌。雪穗和亮司的命运从第一页就交织在一起。",
                       "东野圭吾的叙事太精妙了，多条线索在600多页里慢慢收拢，读到结局的时候整个人都愣住了。震撼、悲凉、绝望，复杂的情绪交织在一起。如果你想看一本真正能击穿你灵魂的推理小说，这本就是。")
            )),

        // ── 汽车 + 母婴 + 健康 (5) ──
        ExplorePost(postId = "post_046",
            author = PostAuthor("auth_046", "新手司机小李", "auth46"),
            product = PostProduct("au_001", "汽车", "70迈行车记录仪 — 4K画质实测", listOf(
                "https://picsum.photos/seed/4d47c742/400/600",
                "https://picsum.photos/seed/59a248b8/400/600",
            )),
            content = PostContent("提车第一件事就是装上它",
                listOf("刚提新车就装了这台70迈D08行车记录仪。4K画质真的清晰，白天可以看清十米外的车牌号，夜拍效果也比预期好。",
                       "前后双录双保险，万一被追尾也不怕没证据。ADAS辅助驾驶提醒也挺好用，前车起步和车道偏离都会语音提示。24小时停车监控给了我很多安全感。")
            )),
        ExplorePost(postId = "post_047",
            author = PostAuthor("auth_047", "新晋奶爸阿豪", "auth47"),
            product = PostProduct("bb_001", "母婴", "贝亲宽口径玻璃奶瓶 — 新生儿必备", listOf(
                "https://picsum.photos/seed/24d8bd7d/400/600",
                "https://picsum.photos/seed/658a162/400/600",
            )),
            content = PostContent("老婆说这是买得最对的新生儿用品",
                listOf("宝宝刚出生的时候买了好几个不同品牌的奶瓶，最后老婆和我一致觉得贝亲这款最好用。自然实感奶嘴模拟母乳吸吮的设计，宝宝接受度很高。",
                       "防胀气通气阀很有效，打嗝和吐奶的概率比用其他奶瓶少了很多。宽口径设计冲洗方便，半夜泡奶粉也不会手忙脚乱。准爸妈们可以提前囤一个。")
            )),
        ExplorePost(postId = "post_048",
            author = PostAuthor("auth_048", "健身教练老王", "auth48"),
            product = PostProduct("hl_001", "健康", "飞利浦电动牙刷 — 用了三年的深度反馈", listOf(
                "https://picsum.photos/seed/358cf05a/400/600",
                "https://picsum.photos/seed/180046c4/400/600",
            )),
            content = PostContent("三年了还在用同一把",
                listOf("飞利浦电动牙刷的质量是真的硬。用了三年，每天早晚两次，电池续航依然能撑两周。声波震动清洁效果比手动刷强太多了，洗牙频率从一年两次变成一年一次。如果你想入门电动牙刷或者替换手里的旧款，飞利浦HX2471是个稳妥的选择。")
            )),
        ExplorePost(postId = "post_049",
            author = PostAuthor("auth_049", "办公桌面美学", "auth49"),
            product = PostProduct("of_001", "办公", "罗技K380键盘 — 写代码和写稿的最佳搭档", listOf(
                "https://picsum.photos/seed/277aa29b/400/600",
                "https://picsum.photos/seed/3218d715/400/600",
            )),
            content = PostContent("买了一把给三个设备用",
                listOf("作为同时用Mac、iPad和Windows的人，K380一键切换三个设备的功能太实用了。圆润按键手感很好，比那种又薄又硬的薄膜键盘舒服多了。",
                       "两节七号电池用了一年多还没换过。小巧轻便塞进背包不占地方，出差和去图书馆都很方便。229元的价格能买到这种品质的无线键盘，物超所值。")
            )),
        ExplorePost(postId = "post_050",
            author = PostAuthor("auth_050", "桌游爱好者", "auth50"),
            product = PostProduct("ty_001", "玩具", "任天堂Switch OLED — 全家人的快乐源泉", listOf(
                "https://picsum.photos/seed/5938bb10/400/600",
                "https://picsum.photos/seed/28abeea5/400/600",
            )),
            content = PostContent("买之前觉得是智商税，买之后真香",
                listOf("去年双十一入的Switch OLED，现在是我使用频率最高的电子设备。塞尔达传说已经玩了200多个小时，每一次探索都有新的惊喜。",
                       "周末和家人一起玩马里奥派对和舞力全开，妈妈都跟着跳起来了。OLED屏幕比旧版鲜艳太多了，掌机模式下的游戏体验直接拉满。如果你还在犹豫要不要入Switch，我的建议是：闭眼冲，不后悔。")
            )),

        ExplorePost(postId = "post_051",
            author = PostAuthor("auth_051", "瑜伽教练Amy", "auth_051"),
            product = PostProduct("sp_post_051", "运动装备", "Lululemon瑜伽垫 — 我的每日拉伸伴侣", listOf(
                "https://picsum.photos/seed/post_051_1/400/600",
                "https://picsum.photos/seed/post_051_2/400/600",
            )),
            content = PostContent("用了半年的真实感受",
                listOf(                       "用了半年的真实感受",
                       "作为瑜伽教练，我对瑜伽垫的要求比较高。Lululemon这面The Reversible Mat完全满足了我对专业级别的期待。天然橡胶基底抓地力超强，做下犬式的时候完全不会滑。",
                       "PU面层越出汗越防滑，这和普通的瑜伽垫完全不一样。双面设计也很贴心，一面防滑一面吸汗。抗菌处理让垫子用久了也没有异味。",
                       "虽然580元不便宜，但对于每天都要用的人来说绝对值得。如果你也在认真对待自己的瑜伽练习，这面垫子不会让你失望。")
            )),
        ExplorePost(postId = "post_052",
            author = PostAuthor("auth_052", "健身达人阿杰", "auth_052"),
            product = PostProduct("sp_post_052", "运动装备", "云麦筋膜枪 — 健身后必备的放松神器", listOf(
                "https://picsum.photos/seed/post_052_1/400/600",
                "https://picsum.photos/seed/post_052_2/400/600",
            )),
            content = PostContent("每次练完腿都会用筋膜枪放松",
                listOf(                       "每次练完腿都会用筋膜枪放松",
                       "以前健身后肌肉酸痛要好几天才能恢复，买了云麦筋膜枪Pro Max之后这个恢复时间直接缩短了一半。12mm的深层振幅可以打到深筋膜层，那种酸爽的感觉让人又爱又恨。",
                       "无刷电机真的很安静，晚上用也不会吵到家人。6个不同的按摩头针对不同部位，我常用的就圆头和扁头两种。699元对于经常健身的人来说是很划算的投资。")
            )),
        ExplorePost(postId = "post_053",
            author = PostAuthor("auth_053", "减脂女孩小鹿", "auth_053"),
            product = PostProduct("sp_post_053", "运动装备", "Keep智能跳绳 — 让跳绳变有趣的科技产品", listOf(
                "https://picsum.photos/seed/post_053_1/400/600",
                "https://picsum.photos/seed/post_053_2/400/600",
            )),
            content = PostContent("每天在家跳两千个",
                listOf(                       "每天在家跳两千个",
                       "之前一直觉得跳绳很无聊，直到入手了Keep智能跳绳。蓝牙连接APP可以记录每次的数据，看着燃脂的卡路里数字往上涨，特别有动力。无绳模式也很实用，在家跳完全不扰楼下邻居。",
                       "Keep的跳绳课程也很丰富，跟着音乐节奏跳比一个人瞎跳有趣多了。79元的价格加上免费课程，性价比真的很高。如果你和我一样想通过跳绳减脂但怕无聊，这个绝对适合你。")
            )),
        ExplorePost(postId = "post_054",
            author = PostAuthor("auth_054", "居家健身党小陈", "auth_054"),
            product = PostProduct("sp_post_054", "运动装备", "迪卡侬哑铃 — 居家健身的性价比之王", listOf(
                "https://picsum.photos/seed/post_054_1/400/600",
                "https://picsum.photos/seed/post_054_2/400/600",
            )),
            content = PostContent("200块就能拥有齐全的哑铃组",
                listOf(                       "200块就能拥有齐全的哑铃组",
                       "疫情期间入的这对迪卡侬可调节哑铃，一直用到现在。旋钮快调切换重量很方便，2.5kg到10kg无极调节覆盖了大部分居家训练需求。包胶外层安静不伤地板，六角造型也不会滚来滚去。",
                       "299元的定价对于一对覆盖多个重量级别的哑铃来说太划算了。现在每周三次力量训练都靠它，手臂和肩膀的线条已经明显了很多。如果你是居家健身新手，强烈推荐从这对哑铃开始。")
            )),
        ExplorePost(postId = "post_055",
            author = PostAuthor("auth_055", "篮球少年TD", "auth_055"),
            product = PostProduct("sp_post_055", "运动装备", "Spalding篮球 — 陪伴我一年的训练好球", listOf(
                "https://picsum.photos/seed/post_055_1/400/600",
                "https://picsum.photos/seed/post_055_2/400/600",
            )),
            content = PostContent("在水泥地上打了一年",
                listOf(                       "在水泥地上打了一年",
                       "斯伯丁74-604Y这个球我打了一年了，手感越来越舒服。PU合成皮在室外水泥地耐磨性不错，没有出现大面积的掉皮。丁基橡胶内胆气密性也很好，一个月充一次气就够了。",
                       "标准7号球的手感比那些几十块的超市球好太多了。投篮回弹刚刚好，运球时球不会乱跳。如果你想要一个室内外通用的训练球，199元的斯伯丁绝对是性价比之王。")
            )),
        ExplorePost(postId = "post_056",
            author = PostAuthor("auth_056", "程序员老王", "auth_056"),
            product = PostProduct("of_post_056", "办公", "乐歌升降桌 — 拯救了我的腰", listOf(
                "https://picsum.photos/seed/post_056_1/400/600",
                "https://picsum.photos/seed/post_056_2/400/600",
            )),
            content = PostContent("站坐交替工作两个月的变化",
                listOf(                       "站坐交替工作两个月的变化",
                       "作为一个一天坐十几个小时的程序员，腰已经发出了各种抗议。买了乐歌升降桌之后开始尝试站坐交替工作，坚持了两个月腰疼明显减轻了很多。双电机升降很平稳，上面的显示器和杯子完全不会晃动。",
                       "三档记忆高度一键切换，早上站着码代码，下午累了坐下来继续。1699元对于一个每天都会用的办公设备来说是很合理的一笔投资。强烈推荐给和我一样久坐办公的朋友。")
            )),
        ExplorePost(postId = "post_057",
            author = PostAuthor("auth_057", "产品经理小李", "auth_057"),
            product = PostProduct("of_post_057", "办公", "科大讯飞录音笔 — 会议记录再也不用愁", listOf(
                "https://picsum.photos/seed/post_057_1/400/600",
                "https://picsum.photos/seed/post_057_2/400/600",
            )),
            content = PostContent("开会时打开它就不用记笔记了",
                listOf(                       "开会时打开它就不用记笔记了",
                       "作为产品经理每周都要开很多会，科大讯飞B1录音笔真的救了我。AI转写准确率高达98%，一小时的会议五分钟就能出完整稿子。支持中英日韩多语种，跟海外团队开会也不怕遗漏信息。",
                       "双麦克风阵列拾音很清晰，就算坐在会议室的角落也能录得很清楚。399元的价格换来了每次开会的轻松，太值了。")
            )),
        ExplorePost(postId = "post_058",
            author = PostAuthor("auth_058", "二胎妈妈花花", "auth_058"),
            product = PostProduct("of_post_058", "办公", "惠普连供打印机 — 家里有娃的必备", listOf(
                "https://picsum.photos/seed/post_058_1/400/600",
                "https://picsum.photos/seed/post_058_2/400/600",
            )),
            content = PostContent("每个月要打印几十页作业",
                listOf(                       "每个月要打印几十页作业",
                       "家里两个娃每周都有打印不完的作业和试卷，惠普Smart Tank 585完全解决了我老往打印店跑的烦恼。自带墨水可以印4000多页，半年了还没加过墨。微信小程序打印也很方便，老师在群里发的文件直接在手机上点一下就出来了。",
                       "彩色打印效果也不错，孩子的手抄报和美术作业都能在家搞定。如果你家也有上学的小朋友，投资一台连供打印机会让你省心很多。")
            )),
        ExplorePost(postId = "post_059",
            author = PostAuthor("auth_059", "考证党小王", "auth_059"),
            product = PostProduct("of_post_059", "办公", "得力签字笔 — 好用不贵的办公神器", listOf(
                "https://picsum.photos/seed/post_059_1/400/600",
                "https://picsum.photos/seed/post_059_2/400/600",
            )),
            content = PostContent("备考三个月写了二十支",
                listOf(                       "备考三个月写了二十支",
                       "备考期间每天要用掉一支笔，得力0.5mm签字笔是我试过性价比最高的。书写顺滑不刮纸，大容量墨囊比普通笔芯耐用多了。按动设计很方便，不需要找笔帽。",
                       "29元12支装可以用很久，考试带了两支备用一支都没用上。如果你也在备考或者需要大量书写，得力这款笔绝对值得囤。")
            )),
        ExplorePost(postId = "post_060",
            author = PostAuthor("auth_060", "桌面极简主义者", "auth_060"),
            product = PostProduct("of_post_060", "办公", "北弧显示器支架 — 桌面瞬间清爽了", listOf(
                "https://picsum.photos/seed/post_060_1/400/600",
                "https://picsum.photos/seed/post_060_2/400/600",
            )),
            content = PostContent("装上去那一刻太治愈了",
                listOf(                       "装上去那一刻太治愈了",
                       "之前桌面总是乱糟糟的因为显示器占了一大片位置。装了北弧双屏支架后桌面瞬间清空了，气压助力单手就能调整高度和角度。横竖屏切换也很方便，写代码用竖屏看文档效率直接翻倍。",
                       "299元的价格换来的桌面整洁和工作效率提升太划算了。内置的线缆收纳设计也让我桌面上看不到一根线。")
            )),
        ExplorePost(postId = "post_061",
            author = PostAuthor("auth_061", "积木控大飞", "auth_061"),
            product = PostProduct("ty_post_061", "玩具", "乐高兰博基尼 — 拼了整整三天", listOf(
                "https://picsum.photos/seed/post_061_1/400/600",
                "https://picsum.photos/seed/post_061_2/400/600",
            )),
            content = PostContent("3696粒的终极挑战",
                listOf(                       "3696粒的终极挑战",
                       "乐高机械组兰博基尼是我拼过最过瘾的一套积木。花了三个晚上加一个周末，总共大概二十多个小时才拼完。可动的V12引擎和八速变速箱设计精妙到让人惊叹。",
                       "拼完放在客厅展示柜里，朋友来了都以为是模型不是积木。1:8的比例细节还原度极高。如果你喜欢机械结构又爱拼积木，这套兰博基尼绝对是值得收藏的神作。")
            )),
        ExplorePost(postId = "post_062",
            author = PostAuthor("auth_062", "盲盒少女喵喵", "auth_062"),
            product = PostProduct("ty_post_062", "玩具", "泡泡玛特Molly盲盒 — 入了坑就出不来", listOf(
                "https://picsum.photos/seed/post_062_1/400/600",
                "https://picsum.photos/seed/post_062_2/400/600",
            )),
            content = PostContent("已经收集了二十几个",
                listOf(                       "已经收集了二十几个",
                       "从第一个Molly星座系列开始入坑，现在已经攒了二十多个了。每次拆盲盒的惊喜感真的上瘾。Molly的可爱造型和精致做工对得起这个价格，摆满一整个展示架的时候特别有成就感。",
                       "唯一的建议是控制好预算，不要像我一样一上头就买一盒。如果你喜欢可爱的手办又享受收集的乐趣，泡泡玛特绝对不会让你失望。")
            )),
        ExplorePost(postId = "post_063",
            author = PostAuthor("auth_063", "主机游戏推荐官", "auth_063"),
            product = PostProduct("ty_post_063", "玩具", "PS5次世代主机 — 玩了半年的深度体验", listOf(
                "https://picsum.photos/seed/post_063_1/400/600",
                "https://picsum.photos/seed/post_063_2/400/600",
            )),
            content = PostContent("买PS5可能是今年最正确的决定",
                listOf(                       "买PS5可能是今年最正确的决定",
                       "DualSense手柄的触觉反馈和自适应扳机真的把沉浸感拉满了。玩战神的时候每一箭射出都能感受到弦的张力，这种体验是其他任何主机都给不了的。",
                       "独占大作的质量确实高，最后生还者、战神、蜘蛛侠都值得反复品味。SSD秒加载是我最惊喜的地方，传送点和重启游戏几乎感觉不到等待。如果你喜欢故事驱动的3A大作，PS5不会让你失望。")
            )),
        ExplorePost(postId = "post_064",
            author = PostAuthor("auth_064", "胶佬认证博主", "auth_064"),
            product = PostProduct("ty_post_064", "玩具", "高达MG元祖RX-78-2 — 入坑胶佬的第一步", listOf(
                "https://picsum.photos/seed/post_064_1/400/600",
                "https://picsum.photos/seed/post_064_2/400/600",
            )),
            content = PostContent("素组也能这么帅",
                listOf(                       "素组也能这么帅",
                       "MG Ver.3.0的元祖高达是我拼过的第一款MG，分件精细到让人感动。素组加渗线就能达到非常不错的完成度。关节的联动设计很巧妙，摆Pose的时候能感受到机械结构的美感。",
                       "299元的价格对MG来说很亲民了。拼完放在电脑桌旁边，每天看着就心情好。如果你也想入高达的坑，从元祖开始是最经典的选择。")
            )),
        ExplorePost(postId = "post_065",
            author = PostAuthor("auth_065", "桌游爱好者阿泽", "auth_065"),
            product = PostProduct("ty_post_065", "玩具", "孩之宝大富翁 — 朋友聚会的保留节目", listOf(
                "https://picsum.photos/seed/post_065_1/400/600",
                "https://picsum.photos/seed/post_065_2/400/600",
            )),
            content = PostContent("每次聚会最后的高潮环节",
                listOf(                       "每次聚会最后的高潮环节",
                       "大富翁真的是永远的神。每次朋友聚会到了后半段，有人喊一句来一局大富翁，大家就全体兴奋。策略博弈加上运气的成分，永远不会无聊。买地建房的策略深度比想象中大很多。",
                       "中文版的设计贴心，规则清晰易懂。两个小时一局刚好，适合4-5个人玩。199元的盒装桌游买了几年了还跟新的一样。")
            )),
        ExplorePost(postId = "post_066",
            author = PostAuthor("auth_066", "孝顺女儿小美", "auth_066"),
            product = PostProduct("hl_post_066", "健康", "欧姆龙血压计 — 给爸妈买的最实用的礼物", listOf(
                "https://picsum.photos/seed/post_066_1/400/600",
                "https://picsum.photos/seed/post_066_2/400/600",
            )),
            content = PostContent("爸妈每天都要测一次",
                listOf(                       "爸妈每天都要测一次",
                       "给爸妈买了欧姆龙上臂式血压计，现在他们每天早晚都会测一次，数据还能蓝牙传到我手机上。大屏显示操作简单，老人家自己就能完成测量。双人60组记忆分别管理，爸妈的数据互不干扰。",
                       "299元的价格换来的是一份安心。上次检测到血压偏高及时就医，避免了更严重的问题。如果你父母的健康你也在关注，血压计真的是最实用的孝心礼物。")
            )),
        ExplorePost(postId = "post_067",
            author = PostAuthor("auth_067", "准妈妈日记", "auth_067"),
            product = PostProduct("hl_post_067", "健康", "鱼跃血糖仪 — 孕期控糖必备", listOf(
                "https://picsum.photos/seed/post_067_1/400/600",
                "https://picsum.photos/seed/post_067_2/400/600",
            )),
            content = PostContent("整个孕期都在用",
                listOf(                       "整个孕期都在用",
                       "怀宝宝查出了妊娠期糖尿病就开始用鱼跃血糖仪每天监测。免调码设计插上试纸就能用，语音播报结果很方便。精准度也很高，和医院测出来的数值基本一致。",
                       "69元五十片试纸的套装对于需要频繁测血糖的人来说很划算。如果你也在孕期需要监测血糖或者要送长辈一个血糖仪，鱼跃是性价比最高的选择。")
            )),
        ExplorePost(postId = "post_068",
            author = PostAuthor("auth_068", "新车日记", "auth_068"),
            product = PostProduct("hl_post_068", "健康", "70迈行车记录仪 — 新车落地必装", listOf(
                "https://picsum.photos/seed/post_068_1/400/600",
                "https://picsum.photos/seed/post_068_2/400/600",
            )),
            content = PostContent("提车第二周就装上了",
                listOf(                       "提车第二周就装上了",
                       "新车落地第一周先去贴了膜，第二周就迫不及待装了这台70迈D08行车记录仪。4K画质真的不是噱头，阴天和晚上都能拍清楚前车的车牌号。前后双录给了我这个新手司机很大的安全感。",
                       "499元的价格买到4K前后双录的行车记录仪真的很值。停车监控功能虽然要多接一根线，但想想万一被人刮蹭找不到人，这个功能就太重要了。")
            )),
        ExplorePost(postId = "post_069",
            author = PostAuthor("auth_069", "宝妈育儿经", "auth_069"),
            product = PostProduct("hl_post_069", "健康", "花王纸尿裤 — 宝宝红屁屁终于好了", listOf(
                "https://picsum.photos/seed/post_069_1/400/600",
                "https://picsum.photos/seed/post_069_2/400/600",
            )),
            content = PostContent("换了三个牌子最终选择了花王",
                listOf(                       "换了三个牌子最终选择了花王",
                       "宝宝刚出生的时候红屁屁特别严重，换了三个牌子的纸尿裤都没用。用了花王妙而舒之后情况明显好转，3D凹凸内层设计确实能快速吸收尿液保持干爽。超薄的0.2cm芯体夏天也不闷热，再也没出现过红屁屁。",
                       "虽然比国产纸尿裤贵一些，但为了宝宝舒服这点钱值得花。如果你家宝宝也有红屁屁的困扰，可以试试花王。")
            )),
        ExplorePost(postId = "post_070",
            author = PostAuthor("auth_070", "孕期营养指南", "auth_070"),
            product = PostProduct("hl_post_070", "健康", "Swisse柠檬酸钙 — 孕期补钙必备", listOf(
                "https://picsum.photos/seed/post_070_1/400/600",
                "https://picsum.photos/seed/post_070_2/400/600",
            )),
            content = PostContent("整个孕期都在吃",
                listOf(                       "整个孕期都在吃",
                       "怀孕后医生建议每天补钙，选了Swisse柠檬酸钙因为温和不伤胃。整个孕期没有出现抽筋的情况，产后去检查骨密度也没有下降。维生素D3帮助钙吸收的配方很科学。",
                       "150粒大瓶装可以吃好几个月，119元的价格在进口钙片里性价比很高。如果你在备孕或者已经怀孕，补钙一定要重视起来。")
            )),
        ExplorePost(postId = "post_071",
            author = PostAuthor("auth_071", "跑步爱好者大刘", "auth_071"),
            product = PostProduct("sh2_post_071", "运动鞋", "安踏C37 4.0 — 百元级缓震跑鞋天花板", listOf(
                "https://picsum.photos/seed/post_071_1/400/600",
                "https://picsum.photos/seed/post_071_2/400/600",
            )),
            content = PostContent("穿了一个月的深度体验",
                listOf(                       "穿了一个月的深度体验",
                       "之前一直穿某进口品牌的跑鞋，朋友推荐安踏C37的时候还半信半疑。穿了才知道，C37的软硬度调校真的精准，走路和慢跑的脚感完全不同——走路软弹舒适，跑步有足够的支撑。",
                       "469元的价格在缓震跑鞋里真的很能打。贾卡网面透气也好，夏天跑步不会捂脚。如果你想买一双性价比高的入门缓震跑鞋，C37绝对不会让你后悔。")
            )),
        ExplorePost(postId = "post_072",
            author = PostAuthor("auth_072", "帆布鞋控", "auth_072"),
            product = PostProduct("sh2_post_072", "运动鞋", "Converse Chuck 70 — 穿了三年的复古帆布鞋", listOf(
                "https://picsum.photos/seed/post_072_1/400/600",
                "https://picsum.photos/seed/post_072_2/400/600",
            )),
            content = PostContent("三年了鞋底才磨平",
                listOf(                       "三年了鞋底才磨平",
                       "Converse Chuck 70比普通款的品质好太多了。加厚的帆布鞋面和OrthoLite鞋垫让脚感上了不止一个档次。穿了三年鞋底才磨平，这个品质在同价位帆布鞋里很少见。",
                       "米白色百搭所有衣服，夏天光腿穿配短裤就是满分休闲穿搭。虽然价格比普通Chuck贵一点，但考虑到三倍的寿命，反而更划算。")
            )),
        ExplorePost(postId = "post_073",
            author = PostAuthor("auth_073", "宅家星人小王", "auth_073"),
            product = PostProduct("sh2_post_073", "运动鞋", "南极人家居服 — 宅家的快乐源泉", listOf(
                "https://picsum.photos/seed/post_073_1/400/600",
                "https://picsum.photos/seed/post_073_2/400/600",
            )),
            content = PostContent("穿上就不想出门",
                listOf(                       "穿上就不想出门",
                       "南极人这套纯棉家居服是我买过最舒服的睡衣。100%纯棉亲肤柔软，宽松版型完全不拘束。弹力腰带很舒服不勒肚子，在家蜗居一整天都不会觉得闷。",
                       "129元一套的价格真的不贵，现在又回购了一套换着穿。如果你也喜欢周末在家宅着看书追剧，一套舒服的家居服绝对能提升幸福感。")
            )),
        ExplorePost(postId = "post_074",
            author = PostAuthor("auth_074", "大学生小张", "auth_074"),
            product = PostProduct("sh2_post_074", "运动鞋", "小米90分行李箱 — 毕业旅行全靠它", listOf(
                "https://picsum.photos/seed/post_074_1/400/600",
                "https://picsum.photos/seed/post_074_2/400/600",
            )),
            content = PostContent("带着它去了五个城市",
                listOf(                       "带着它去了五个城市",
                       "大一的时候买了小米90分20寸PC行李箱，大三了还在用。德国拜耳PC材质真的轻韧抗压，在火车行李架上被压了无数次表面也只是轻微划痕。TPE减震万向轮拖过石子路也不卡顿。",
                       "349元的价格对于能用好几年的行李箱来说超级划算。20寸刚好可以带上飞机，毕业旅行全靠它。小身材大容量，设计也很简洁耐看。")
            )),
        ExplorePost(postId = "post_075",
            author = PostAuthor("auth_075", "科学护肤日记", "auth_075"),
            product = PostProduct("sh2_post_075", "运动鞋", "珀莱雅双抗精华 — 成分党亲测有效", listOf(
                "https://picsum.photos/seed/post_075_1/400/600",
                "https://picsum.photos/seed/post_075_2/400/600",
            )),
            content = PostContent("用了一个月皮肤检测报告",
                listOf(                       "用了一个月皮肤检测报告",
                       "作为一个喜欢研究成分的护肤党，珀莱雅双抗的配方确实做得很良心。虾青素和麦角硫因的抗氧化组合白天用能明显感觉到底妆不容易暗沉。脱羧肌肽的抗糖化效果需要更长时间才能看到。",
                       "质地是轻润乳液状，油皮在夏天叠加防晒也不闷。如果你开始关注初老问题和抗氧化，239元一瓶的双抗精华是很值得尝试的入门产品。")
            )),
        ExplorePost(postId = "post_076",
            author = PostAuthor("auth_076", "零食超市测评", "auth_076"),
            product = PostProduct("sh2_post_076", "运动鞋", "旺旺大礼包 — 童年回忆杀", listOf(
                "https://picsum.photos/seed/post_076_1/400/600",
                "https://picsum.photos/seed/post_076_2/400/600",
            )),
            content = PostContent("吃一口就回到小时候",
                listOf(                       "吃一口就回到小时候",
                       "过年买了两箱旺旺大礼包，打开仙贝的那一瞬间整个人都回到了小学。仙贝和大米饼还是小时候的味道，酥脆咸香完全没变。黑芝麻雪饼是近几年新增的口味也很不错。",
                       "59元一箱包括仙贝、雪饼、小小酥、浪味仙，春节送小孩或者自己解馋都很合适。虽然现在零食的选择多了，但旺旺的地位是真的无法替代。")
            )),
        ExplorePost(postId = "post_077",
            author = PostAuthor("auth_077", "办公室零食达人", "auth_077"),
            product = PostProduct("sh2_post_077", "运动鞋", "百草味零食大礼包 — 同事聚会必买", listOf(
                "https://picsum.photos/seed/post_077_1/400/600",
                "https://picsum.photos/seed/post_077_2/400/600",
            )),
            content = PostContent("每次团建必买一箱",
                listOf(                       "每次团建必买一箱",
                       "办公室每次有人生日或者团建，百草味零食大礼包都会出现在桌上。满满一箱有甜有咸各种零食，总能满足所有人的口味。独立包装设计让分享更方便卫生。",
                       "99元一箱十五种以上的零食组合，比单独买划算多了。每月还有更新，这次开箱芒果干和炭烧腰果特别好吃。")
            )),
        ExplorePost(postId = "post_078",
            author = PostAuthor("auth_078", "师大日常记", "auth_078"),
            product = PostProduct("sh2_post_078", "运动鞋", "无印良品帆布包 — 大学四年的日常", listOf(
                "https://picsum.photos/seed/post_078_1/400/600",
                "https://picsum.photos/seed/post_078_2/400/600",
            )),
            content = PostContent("大一买到大四还在用",
                listOf(                       "大一买到大四还在用",
                       "大一在实体店买的MUJI帆布托特包，用到大四还跟新的一样。厚实的纯棉帆布越用越有质感，从来没有掉线头或者脱线的烦恼。容量装几本书和便当盒完全够。",
                       "58元一个真的不贵，而且款式极简百搭。很多同学看到我背也去买了，质量是真的过硬。如果你在找一个可以每天用的极简帆布包，MUJI这款可以直接下单。")
            )),
        ExplorePost(postId = "post_079",
            author = PostAuthor("auth_079", "敏感肌日记", "auth_079"),
            product = PostProduct("sh2_post_079", "运动鞋", "全棉时代棉柔巾 — 我的洗脸方式变了", listOf(
                "https://picsum.photos/seed/post_079_1/400/600",
                "https://picsum.photos/seed/post_079_2/400/600",
            )),
            content = PostContent("用了两年没再买过毛巾",
                listOf(                       "用了两年没再买过毛巾",
                       "自从用了全棉时代棉柔巾擦脸之后再也没有用过毛巾。100%纯棉不掉絮不刺激，用完一张再抽下一张干净卫生。干湿两用很方便，沾水之后当湿巾清洁也很到位。",
                       "79元6包的套装可以用很久，敏感肌用着很安心。如果你也在找替代毛巾的洗脸方式，强烈推荐尝试。")
            )),
        ExplorePost(postId = "post_080",
            author = PostAuthor("auth_080", "口腔护理推荐", "auth_080"),
            product = PostProduct("sh2_post_080", "运动鞋", "云南白药牙膏 — 牙龈出血终于好了", listOf(
                "https://picsum.photos/seed/post_080_1/400/600",
                "https://picsum.photos/seed/post_080_2/400/600",
            )),
            content = PostContent("刷了两周牙龈就不出血了",
                listOf(                       "刷了两周牙龈就不出血了",
                       "之前每次刷牙牙龈都会出血，换了云南白药牙膏用了大概两周就不出血了。中药活性成分确实有效，口腔溃疡的时候涂一点在溃疡处也能缓解疼痛。",
                       "49元两支的价格比进口牙膏便宜太多了，效果却一点不差。如果你也有牙龈出血或者口腔溃疡的困扰，这个牙膏可以试试。")
            )),
        ExplorePost(postId = "post_081",
            author = PostAuthor("auth_081", "自由职业者小康", "auth_081"),
            product = PostProduct("dg2_post_081", "数码", "MacBook Air M3 — 轻薄本的天花板", listOf(
                "https://picsum.photos/seed/post_081_1/400/600",
                "https://picsum.photos/seed/post_081_2/400/600",
            )),
            content = PostContent("带它去过十几个咖啡厅",
                listOf(                       "带它去过十几个咖啡厅",
                       "作为自由职业者，我几乎每天都在咖啡厅和共享办公空间工作。MacBook Air M3是我用过最适合移动办公的电脑。轻到可以一只手拿着走，续航够我从早工作到晚不需要带充电器。",
                       "M3芯片的性能应对WPS、Figma、轻度视频剪辑都很流畅。蝶式键盘打字手感也好。如果你需要一台可以每天背着的笔记本，Air M3是目前最好的选择。")
            )),
        ExplorePost(postId = "post_082",
            author = PostAuthor("auth_082", "猫奴日记", "auth_082"),
            product = PostProduct("dg2_post_082", "数码", "戴森V15吸尘器 — 养猫人的救星", listOf(
                "https://picsum.photos/seed/post_082_1/400/600",
                "https://picsum.photos/seed/post_082_2/400/600",
            )),
            content = PostContent("家里两只猫掉毛太严重了",
                listOf(                       "家里两只猫掉毛太严重了",
                       "家里两只猫换季的时候掉毛简直成灾，戴森V15的激光探测功能让我震惊了——原来每天地上有那么多肉眼看不见的猫毛和皮屑。吸完之后床垫和沙发都感觉焕然一新。",
                       "转换各种吸头可以清洁沙发缝隙、窗帘、猫爬架，一机多用。虽然价格不便宜，但对于养猫家庭来说这台吸尘器的价值远超它的价格。")
            )),
        ExplorePost(postId = "post_083",
            author = PostAuthor("auth_083", "精致生活日记", "auth_083"),
            product = PostProduct("dg2_post_083", "数码", "富安娜四件套 — 睡眠品质的飞跃", listOf(
                "https://picsum.photos/seed/post_083_1/400/600",
                "https://picsum.photos/seed/post_083_2/400/600",
            )),
            content = PostContent("换上之后每天都不想早起",
                listOf(                       "换上之后每天都不想早起",
                       "之前一直睡的是租房时随便买的一百多块的四件套。换了富安娜这个60支长绒棉贡缎之后才意识到好的床品有多重要。面料顺滑得像五星级酒店，新疆长绒棉的透气性让我夏天也不会睡着睡着出汗。",
                       "洗了几次也没有褪色起球，活性印染的工艺确实到位。如果你也想提升睡眠品质，从好床品开始是最直接的方式。")
            )),
        ExplorePost(postId = "post_084",
            author = PostAuthor("auth_084", "减脂打卡人", "auth_084"),
            product = PostProduct("dg2_post_084", "数码", "小米体重秤 — 坚持了三个月的减肥日志", listOf(
                "https://picsum.photos/seed/post_084_1/400/600",
                "https://picsum.photos/seed/post_084_2/400/600",
            )),
            content = PostContent("每天早晨第一件事就是站上去",
                listOf(                       "每天早晨第一件事就是站上去",
                       "开始减肥计划的第一天就买了小米体重秤2，每天早上空腹称重已经坚持了快三个月。蓝牙连接米家APP自动记录体重和体脂率，看着数据曲线一天天下降特别有成就感。",
                       "高精度传感器到50g级别，喝一杯水都能检测出来变化。支持全家人独立数据管理，我爸妈也在用。59元的价格换来的是一个持续的减肥动力。")
            )),
        ExplorePost(postId = "post_085",
            author = PostAuthor("auth_085", "读书笔记控", "auth_085"),
            product = PostProduct("dg2_post_085", "数码", "《小王子》中法双语版 — 枕边放了一年的书", listOf(
                "https://picsum.photos/seed/post_085_1/400/600",
                "https://picsum.photos/seed/post_085_2/400/600",
            )),
            content = PostContent("睡前翻两页心灵就安宁了",
                listOf(                       "睡前翻两页心灵就安宁了",
                       "《小王子》是我书架上翻得最旧的一本书。中法双语版的设计让我在阅读的时候还能顺带学几句法语。圣埃克苏佩里的文字有一种沉静的力量，每次睡前翻两页就觉得心灵被净化了。",
                       "精装版的插画也很美，作为礼物送给朋友非常合适。如果你还没有读过《小王子》或者只看过中文版，双语版值得收藏。")
            )),
        ExplorePost(postId = "post_086",
            author = PostAuthor("auth_086", "读书分享号", "auth_086"),
            product = PostProduct("dg2_post_086", "数码", "《人类简史》 — 2024年最震撼我的一本书", listOf(
                "https://picsum.photos/seed/post_086_1/400/600",
                "https://picsum.photos/seed/post_086_2/400/600",
            )),
            content = PostContent("花了三个月慢慢啃完",
                listOf(                       "花了三个月慢慢啃完",
                       "赫拉利的《人类简史》是我今年读过最开脑洞的书。从认知革命到科学革命，七万年的智人历史被他写得跌宕起伏一气呵成。读完之后对很多习以为常的社会制度有了完全不同的视角。",
                       "虽然是学术著作但可读性很强，每天睡前看一小节也不会觉得枯燥。如果今年你只想看一本书让自己变得聪明一点点，读这本就对了。")
            )),
        ExplorePost(postId = "post_087",
            author = PostAuthor("auth_087", "工具控阿飞", "auth_087"),
            product = PostProduct("dg2_post_087", "数码", "Kindle Scribe — 实现了无纸化笔记的梦想", listOf(
                "https://picsum.photos/seed/post_087_1/400/600",
                "https://picsum.photos/seed/post_087_2/400/600",
            )),
            content = PostContent("现在带去图书馆只带一个Kindle",
                listOf(                       "现在带去图书馆只带一个Kindle",
                       "之前去图书馆总要带好几本笔记本和各种颜色的笔。换了Kindle Scribe之后所有笔记都集中在一个设备上了，大屏幕看PDF也完全不需要缩放。手写笔延迟很低，跟在纸上写的体验差不多。",
                       "续航超长一周充一次就够了。如果你是一个喜欢读书又喜欢做笔记的人，Scribe能让你的书包轻一半。")
            )),
        ExplorePost(postId = "post_088",
            author = PostAuthor("auth_088", "独居生活日记", "auth_088"),
            product = PostProduct("dg2_post_088", "数码", "苏泊尔电饭煲 — 独居生活的第一台家电", listOf(
                "https://picsum.photos/seed/post_088_1/400/600",
                "https://picsum.photos/seed/post_088_2/400/600",
            )),
            content = PostContent("一个人也要好好吃饭",
                listOf(                       "一个人也要好好吃饭",
                       "搬出来独居后买的第一件家电就是苏泊尔球釜电饭煲。IH电磁加热煮出来的米饭比之前用普通电饭煲的香太多了，颗粒分明不粘锅。多功能菜单让不会做饭的我也能做出一锅好粥。",
                       "4L容量一到两个人用刚好，24小时预约功能让我早上起床就能吃到热粥。299元的价格对得住苏泊尔这个品牌的质量。如果你也刚开始独居，这绝对是值得投入的第一件家电。")
            )),
        ExplorePost(postId = "post_089",
            author = PostAuthor("auth_089", "厨房小白进化论", "auth_089"),
            product = PostProduct("dg2_post_089", "数码", "美的空气炸锅 — 炸鸡自由终于实现了", listOf(
                "https://picsum.photos/seed/post_089_1/400/600",
                "https://picsum.photos/seed/post_089_2/400/600",
            )),
            content = PostContent("炸鸡薯条烤鸡翅我全做了",
                listOf(                       "炸鸡薯条烤鸡翅我全做了",
                       "美的5L空气炸锅真的是厨房小白的救世主。不需要任何厨艺基础，把腌好的鸡翅扔进去选择对应的模式，等十几分钟就能吃到金黄酥脆的炸鸡翅。免翻面设计省掉了最麻烦的步骤。",
                       "比外面买的油炸食品健康太多了，而且自家做的新鲜放心。如果你也想在家实现垃圾食品自由但又怕胖怕复杂，空气炸锅就是你的最佳选择。")
            )),
        ExplorePost(postId = "post_090",
            author = PostAuthor("auth_090", "国货彩妆分享", "auth_090"),
            product = PostProduct("dg2_post_090", "数码", "花西子蜜粉 — 国货定妆太能打了", listOf(
                "https://picsum.photos/seed/post_090_1/400/600",
                "https://picsum.photos/seed/post_090_2/400/600",
            )),
            content = PostContent("T区出油星人的救星",
                listOf(                       "T区出油星人的救星",
                       "作为一个T区特别爱出油的人，花西子空气蜜粉真的救了我的底妆。微米级粉质细腻到像空气一样，上脸瞬间哑光柔焦。中午只需要在T区补一点就能撑到下班。",
                       "自带的粉扑和小镜子让外出补妆很方便。129元的价格买到的品质完全不输进口大牌三百多的定妆粉。如果你也在找平价好用的定妆产品，国货花西子你值得拥有。")
            )),
        ExplorePost(postId = "post_091",
            author = PostAuthor("auth_091", "瑜伽日常记录", "auth_091"),
            product = PostProduct("cl2_post_091", "服装", "Lululemon瑜伽裤 — 穿了就不想脱", listOf(
                "https://picsum.photos/seed/post_091_1/400/600",
                "https://picsum.photos/seed/post_091_2/400/600",
            )),
            content = PostContent("一周穿了五次",
                listOf(                       "一周穿了五次",
                       "Align系列的裸感面料真的是神。穿上之后完全不觉得勒，做任何瑜伽体式都自由自在。高腰设计包裹感很好，不会卷边也不会下滑。出汗之后也不会贴着皮肤不舒服。",
                       "虽然价格比其他运动裤贵不少，但考虑到每周穿五次的频率和两年的寿命，性价比其实很高。如果你也喜欢瑜伽或者健身，Align是值得投资的第一条瑜伽裤。")
            )),
        ExplorePost(postId = "post_092",
            author = PostAuthor("auth_092", "职场穿搭图鉴", "auth_092"),
            product = PostProduct("cl2_post_092", "服装", "太平鸟阔腿裤 — 通勤穿搭的终极答案", listOf(
                "https://picsum.photos/seed/post_092_1/400/600",
                "https://picsum.photos/seed/post_092_2/400/600",
            )),
            content = PostContent("百搭到每天都在穿",
                listOf(                       "百搭到每天都在穿",
                       "太平鸟这条直筒阔腿裤是今年买得最值的一件衣服。垂感面料轻盈有质感，走路带风的感觉很飒。高腰设计搭配短上衣显腿长，黑色款搭配任何上装都不违和。后腰松紧设计也让长时间坐着办公很舒适。",
                       "359元的价格在这个品质的裤子里算很公道了。如果你和我一样想找一条职场和日常都能穿的百搭裤子，这条就是了。")
            )),
        ExplorePost(postId = "post_093",
            author = PostAuthor("auth_093", "新手妈妈成长记", "auth_093"),
            product = PostProduct("cl2_post_093", "服装", "贝亲奶瓶 — 从出生用到现在", listOf(
                "https://picsum.photos/seed/post_093_1/400/600",
                "https://picsum.photos/seed/post_093_2/400/600",
            )),
            content = PostContent("宝宝最爱的奶瓶",
                listOf(                       "宝宝最爱的奶瓶",
                       "从宝宝出生就开始用贝亲宽口径玻璃奶瓶，现在快一岁了还在用。自然实感奶嘴的设计确实模拟了母乳吸吮的感觉，宝宝接受度非常高。防胀气通气阀也有效，打嗝比之前用其他奶瓶少了很多。",
                       "宽口径设计让冲泡和清洗都很方便，半夜爬起来冲奶粉也不会手忙脚乱。如果你正在为准爸妈准备新生儿用品，贝亲奶瓶一定是必买清单之一。")
            )),
        ExplorePost(postId = "post_094",
            author = PostAuthor("auth_094", "猫主子日常", "auth_094"),
            product = PostProduct("cl2_post_094", "服装", "皇家猫粮 — 我家猫咪吃了一年胖了", listOf(
                "https://picsum.photos/seed/post_094_1/400/600",
                "https://picsum.photos/seed/post_094_2/400/600",
            )),
            content = PostContent("从流浪猫变成了小胖橘",
                listOf(                       "从流浪猫变成了小胖橘",
                       "去年收养了一只瘦得皮包骨头的流浪猫，喂了皇家F32成猫粮，一年下来已经是圆滚滚的小胖橘了。毛发光泽度比之前好了不止一个档次，手感丝滑了好多。便便也从之前的拉稀变成了健康的成型状。",
                       "4kg大袋可以吃两个月，239元的价格在进口猫粮里不算贵。如果你家猫主子挑食或者偏瘦，皇家的精准营养配方值得试试。")
            )),
        ExplorePost(postId = "post_095",
            author = PostAuthor("auth_095", "养宠知识分享", "auth_095"),
            product = PostProduct("cl2_post_095", "服装", "小佩饮水机 — 猫咪终于爱上喝水了", listOf(
                "https://picsum.photos/seed/post_095_1/400/600",
                "https://picsum.photos/seed/post_095_2/400/600",
            )),
            content = PostContent("以前猫只喝我杯子里的水",
                listOf(                       "以前猫只喝我杯子里的水",
                       "养猫的人都知道猫咪不爱喝水的问题，之前猫主子只愿意喝我杯子里的水。买了小佩循环饮水机之后，猫咪现在每天主动跑去喝水，因为循环过滤的水是流动的更新鲜。UV杀菌功能让我不用担心水质问题。",
                       "不锈钢机身清洗方便不生锈，2L容量刚好够一只猫喝一周。如果你也在发愁猫咪不爱喝水可能得泌尿疾病，循环饮水机绝对值得投资。")
            )),
        ExplorePost(postId = "post_096",
            author = PostAuthor("auth_096", "宝妈好物日记", "auth_096"),
            product = PostProduct("cl2_post_096", "服装", "Hape儿童厨房玩具 — 女儿每天都要玩", listOf(
                "https://picsum.photos/seed/post_096_1/400/600",
                "https://picsum.photos/seed/post_096_2/400/600",
            )),
            content = PostContent("两岁生日礼物买得太对了",
                listOf(                       "两岁生日礼物买得太对了",
                       "女儿两岁生日送了Hape的木制儿童厨房，现在已经快一年了每天都要玩。实木材质安全环保没有异味，边角都打磨得很圆润不会伤到小朋友。仿真灶台和水槽设计让她模仿大人做饭的游戏变得特别投入。",
                       "虽然价格不便宜，但能玩好几年而且木质玩具的品质是塑料玩具没法比的。如果你在给小朋友挑选生日礼物，Hape的儿童厨房是非常棒的选择。")
            )),
        ExplorePost(postId = "post_097",
            author = PostAuthor("auth_097", "孝心日志", "auth_097"),
            product = PostProduct("cl2_post_097", "服装", "欧姆龙血压计 — 送给老爸的父亲节礼物", listOf(
                "https://picsum.photos/seed/post_097_1/400/600",
                "https://picsum.photos/seed/post_097_2/400/600",
            )),
            content = PostContent("老爸收到时说这礼物真贴心",
                listOf(                       "老爸收到时说这礼物真贴心",
                       "父亲节送了老爸欧姆龙上臂式血压计，他说这是他收到过最实用的礼物。大屏幕显示和语音播报让老人家自己操作完全没问题，每天早晚都会量一次。数据能蓝牙同步到我手机上，万一有异常能第一时间发现。",
                       "如果你也在纠结送父母什么礼物，强烈推荐血压计。299元换来的是一份持续的安心和健康守护。")
            )),
        ExplorePost(postId = "post_098",
            author = PostAuthor("auth_098", "羽毛球俱乐部", "auth_098"),
            product = PostProduct("cl2_post_098", "服装", "YONEX天斧100ZZ — 俱乐部队友都在问", listOf(
                "https://picsum.photos/seed/post_098_1/400/600",
                "https://picsum.photos/seed/post_098_2/400/600",
            )),
            content = PostContent("换了新拍之后杀球更有力了",
                listOf(                       "换了新拍之后杀球更有力了",
                       "原来用的入门拍打了一年想升级，在球友推荐下入了YONEX天斧100ZZ。头重设计配合Namd新次元碳素的弹性，高远球和杀球的爆发力提升非常明显。中杆硬度偏高需要一定力量基础，但用顺了之后的精准度让人很享受。",
                       "如果你已经打了两年以上的羽毛球想要升级装备，100ZZ是一把能用很长一段时间的好拍子。")
            )),
        ExplorePost(postId = "post_099",
            author = PostAuthor("auth_099", "成分党护肤", "auth_099"),
            product = PostProduct("cl2_post_099", "服装", "Swisse葡萄籽精华 — 吃了三个月的反馈", listOf(
                "https://picsum.photos/seed/post_099_1/400/600",
                "https://picsum.photos/seed/post_099_2/400/600",
            )),
            content = PostContent("皮肤确实变细腻了",
                listOf(                       "皮肤确实变细腻了",
                       "被朋友安利了Swisse葡萄籽精华，坚持每天一粒吃了三个月。最明显的变化是皮肤肉眼可见变细腻了，之前脸颊有些轻微的色斑也淡了一些。作为抗氧化保健品它含有的原花青素浓度比市面很多同类产品都高。",
                       "119元一瓶可以吃好几个月，性价比在进口保健品里算很不错的。如果你也想内调外养改善皮肤状态，葡萄籽精华是可以尝试的一个方向。")
            )),
        ExplorePost(postId = "post_100",
            author = PostAuthor("auth_100", "出差党好物", "auth_100"),
            product = PostProduct("cl2_post_100", "服装", "爱国者移动电源 — 出差旅行必备", listOf(
                "https://picsum.photos/seed/post_100_1/400/600",
                "https://picsum.photos/seed/post_100_2/400/600",
            )),
            content = PostContent("30W快充速度真的爽",
                listOf(                       "30W快充速度真的爽",
                       "自从换了爱国者这款20000mAh快充移动电源之后，出差再也不需要带好几个充电头了。30W快充给iPhone和iPad都能高速充电，同时充两个设备也不会降速。20000mAh可以给我的手机充满四次。",
                       "金属机身散热好不烫手，小巧便携装包里不占空间。如果你出差或者旅行的时候经常需要给多个设备充电，这款移动电源绝对值得入手。")
            )),
    )

    /** 根据产品索引映射到分享帖 */
    fun getPostForIndex(index: Int): ExplorePost = posts[index % posts.size]
}
