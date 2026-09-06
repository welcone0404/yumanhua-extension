package eu.kanade.tachiyomi.extension.zh.yumanhua

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.post
import keiyoushi.source.KeiSource
import okhttp3.FormBody
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

@Source
abstract class Yumanhua : KeiSource() {

    override suspend fun getPopularManga(page: Int): MangasPage =
        getListingPage("/rank/2", page)

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        getListingPage("/rank/5", page)

    private suspend fun getListingPage(
        path: String,
        page: Int,
    ): MangasPage {
        if (page > 1) return MangasPage(emptyList(), false)

        val url = "$baseUrl$path"
        val response = client.get(url)
        val html = response.use { it.body.string() }
        val document = Jsoup.parse(html, url)

        val mangas =
            document.select("a[href]")
                .mapNotNull { a ->
                    val href = a.attr("href").substringBefore("?").trim()
                    if (!href.matches(Regex("^/[A-Za-z0-9_-]+/$"))) {
                        return@mapNotNull null
                    }

                    val img =
                        a.selectFirst("img[data-src], img[src]")

                    val title =
                        a.selectFirst(".e-title, .name, .title, h3, h4, p")
                            ?.text()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: img?.attr("title")?.trim()?.takeIf { it.isNotBlank() }
                            ?: img?.attr("alt")?.trim()?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null

                    val thumbnail =
                        img?.absUrl("data-src")
                            ?.takeIf { it.isNotBlank() }
                            ?: img?.absUrl("src")
                                ?.takeIf {
                                    it.isNotBlank() &&
                                        !it.contains("/static/images/load.gif")
                                }

                    SManga.create().apply {
                        url = href
                        this.title = title
                        if (!thumbnail.isNullOrBlank()) {
                            thumbnail_url = thumbnail
                        }
                    }
                }
                .distinctBy { it.url }

        return MangasPage(
            mangas,
            false,
        )
    }

    private fun traditionalToSimplifiedSearch(text: String): String {
        val map =
            mapOf(
                '萬' to '万',
                '與' to '与',
                '專' to '专',
                '業' to '业',
                '東' to '东',
                '絲' to '丝',
                '丟' to '丢',
                '兩' to '两',
                '嚴' to '严',
                '喪' to '丧',
                '個' to '个',
                '豐' to '丰',
                '臨' to '临',
                '為' to '为',
                '麗' to '丽',
                '舉' to '举',
                '義' to '义',
                '烏' to '乌',
                '樂' to '乐',
                '喬' to '乔',
                '習' to '习',
                '鄉' to '乡',
                '書' to '书',
                '買' to '买',
                '亂' to '乱',
                '爭' to '争',
                '於' to '于',
                '雲' to '云',
                '亞' to '亚',
                '產' to '产',
                '畝' to '亩',
                '親' to '亲',
                '億' to '亿',
                '僅' to '仅',
                '從' to '从',
                '侖' to '仑',
                '倉' to '仓',
                '儀' to '仪',
                '們' to '们',
                '優' to '优',
                '會' to '会',
                '傘' to '伞',
                '偉' to '伟',
                '傳' to '传',
                '傷' to '伤',
                '倫' to '伦',
                '體' to '体',
                '俠' to '侠',
                '侶' to '侣',
                '側' to '侧',
                '偵' to '侦',
                '兒' to '儿',
                '黨' to '党',
                '蒼' to '苍',
                '視' to '视',
                '讀' to '读',
                '蘭' to '兰',
                '關' to '关',
                '興' to '兴',
                '養' to '养',
                '獸' to '兽',
                '內' to '内',
                '岡' to '冈',
                '冊' to '册',
                '寫' to '写',
                '軍' to '军',
                '農' to '农',
                '沖' to '冲',
                '決' to '决',
                '況' to '况',
                '凍' to '冻',
                '淨' to '净',
                '準' to '准',
                '幾' to '几',
                '鳳' to '凤',
                '憑' to '凭',
                '凱' to '凯',
                '擊' to '击',
                '劃' to '划',
                '劉' to '刘',
                '則' to '则',
                '剛' to '刚',
                '創' to '创',
                '刪' to '删',
                '別' to '别',
                '製' to '制',
                '劍' to '剑',
                '劇' to '剧',
                '勁' to '劲',
                '動' to '动',
                '務' to '务',
                '勝' to '胜',
                '勞' to '劳',
                '勢' to '势',
                '勳' to '勋',
                '區' to '区',
                '醫' to '医',
                '華' to '华',
                '協' to '协',
                '單' to '单',
                '賣' to '卖',
                '盧' to '卢',
                '衛' to '卫',
                '卻' to '却',
                '廠' to '厂',
                '廳' to '厅',
                '歷' to '历',
                '壓' to '压',
                '厭' to '厌',
                '縣' to '县',
                '參' to '参',
                '雙' to '双',
                '發' to '发',
                '變' to '变',
                '葉' to '叶',
                '號' to '号',
                '嘆' to '叹',
                '嗎' to '吗',
                '聽' to '听',
                '啟' to '启',
                '員' to '员',
                '吳' to '吴',
                '吶' to '呐',
                '週' to '周',
                '響' to '响',
                '問' to '问',
                '啞' to '哑',
                '喚' to '唤',
                '喪' to '丧',
                '喬' to '乔',
                '單' to '单',
                '噴' to '喷',
                '囉' to '啰',
                '圍' to '围',
                '國' to '国',
                '圖' to '图',
                '圓' to '圆',
                '聖' to '圣',
                '場' to '场',
                '塊' to '块',
                '堅' to '坚',
                '壇' to '坛',
                '壞' to '坏',
                '壯' to '壮',
                '聲' to '声',
                '殼' to '壳',
                '處' to '处',
                '備' to '备',
                '復' to '复',
                '夠' to '够',
                '夢' to '梦',
                '夾' to '夹',
                '奪' to '夺',
                '奮' to '奋',
                '婦' to '妇',
                '媽' to '妈',
                '嬌' to '娇',
                '學' to '学',
                '寶' to '宝',
                '實' to '实',
                '寵' to '宠',
                '審' to '审',
                '寫' to '写',
                '將' to '将',
                '尋' to '寻',
                '對' to '对',
                '導' to '导',
                '壽' to '寿',
                '爾' to '尔',
                '塵' to '尘',
                '層' to '层',
                '屬' to '属',
                '歲' to '岁',
                '島' to '岛',
                '嶺' to '岭',
                '巔' to '巅',
                '帥' to '帅',
                '師' to '师',
                '帳' to '帐',
                '帶' to '带',
                '幫' to '帮',
                '幣' to '币',
                '幹' to '干',
                '庫' to '库',
                '應' to '应',
                '廢' to '废',
                '廣' to '广',
                '開' to '开',
                '異' to '异',
                '棄' to '弃',
                '張' to '张',
                '彌' to '弥',
                '強' to '强',
                '歸' to '归',
                '當' to '当',
                '錄' to '录',
                '徑' to '径',
                '徹' to '彻',
                '憶' to '忆',
                '懷' to '怀',
                '態' to '态',
                '總' to '总',
                '戀' to '恋',
                '惡' to '恶',
                '驚' to '惊',
                '愛' to '爱',
                '慘' to '惨',
                '慶' to '庆',
                '慮' to '虑',
                '戰' to '战',
                '戲' to '戏',
                '戶' to '户',
                '撲' to '扑',
                '執' to '执',
                '擁' to '拥',
                '擇' to '择',
                '擔' to '担',
                '據' to '据',
                '擴' to '扩',
                '擺' to '摆',
                '攜' to '携',
                '敗' to '败',
                '敵' to '敌',
                '數' to '数',
                '斷' to '断',
                '無' to '无',
                '時' to '时',
                '晉' to '晋',
                '曉' to '晓',
                '暫' to '暂',
                '術' to '术',
                '機' to '机',
                '殺' to '杀',
                '雜' to '杂',
                '權' to '权',
                '條' to '条',
                '來' to '来',
                '極' to '极',
                '構' to '构',
                '槍' to '枪',
                '樓' to '楼',
                '標' to '标',
                '樣' to '样',
                '樹' to '树',
                '橋' to '桥',
                '檢' to '检',
                '櫻' to '樱',
                '歡' to '欢',
                '歐' to '欧',
                '殘' to '残',
                '毀' to '毁',
                '氣' to '气',
                '漢' to '汉',
                '湯' to '汤',
                '溝' to '沟',
                '滅' to '灭',
                '滿' to '满',
                '漁' to '渔',
                '潔' to '洁',
                '潛' to '潜',
                '澤' to '泽',
                '濟' to '济',
                '濤' to '涛',
                '濫' to '滥',
                '灣' to '湾',
                '燈' to '灯',
                '靈' to '灵',
                '災' to '灾',
                '爭' to '争',
                '爺' to '爷',
                '牆' to '墙',
                '獨' to '独',
                '獎' to '奖',
                '獵' to '猎',
                '獸' to '兽',
                '環' to '环',
                '現' to '现',
                '產' to '产',
                '畫' to '画',
                '異' to '异',
                '療' to '疗',
                '瘋' to '疯',
                '盡' to '尽',
                '監' to '监',
                '盤' to '盘',
                '盜' to '盗',
                '眾' to '众',
                '睜' to '睁',
                '礦' to '矿',
                '碼' to '码',
                '禮' to '礼',
                '禍' to '祸',
                '種' to '种',
                '稱' to '称',
                '穩' to '稳',
                '窮' to '穷',
                '競' to '竞',
                '筆' to '笔',
                '築' to '筑',
                '簡' to '简',
                '簽' to '签',
                '籠' to '笼',
                '類' to '类',
                '糧' to '粮',
                '紀' to '纪',
                '約' to '约',
                '紅' to '红',
                '級' to '级',
                '紋' to '纹',
                '納' to '纳',
                '紙' to '纸',
                '純' to '纯',
                '紗' to '纱',
                '紛' to '纷',
                '終' to '终',
                '組' to '组',
                '結' to '结',
                '絕' to '绝',
                '統' to '统',
                '絲' to '丝',
                '經' to '经',
                '綁' to '绑',
                '綠' to '绿',
                '維' to '维',
                '網' to '网',
                '練' to '练',
                '縱' to '纵',
                '總' to '总',
                '績' to '绩',
                '繼' to '继',
                '續' to '续',
                '纏' to '缠',
                '罰' to '罚',
                '羅' to '罗',
                '羈' to '羁',
                '習' to '习',
                '翹' to '翘',
                '聖' to '圣',
                '聞' to '闻',
                '聯' to '联',
                '職' to '职',
                '聰' to '聪',
                '肅' to '肃',
                '脫' to '脱',
                '腦' to '脑',
                '臉' to '脸',
                '臺' to '台',
                '舊' to '旧',
                '艦' to '舰',
                '藝' to '艺',
                '藥' to '药',
                '蘇' to '苏',
                '虛' to '虚',
                '蟲' to '虫',
                '蠻' to '蛮',
                '術' to '术',
                '衛' to '卫',
                '裝' to '装',
                '裡' to '里',
                '見' to '见',
                '觀' to '观',
                '規' to '规',
                '覺' to '觉',
                '覽' to '览',
                '觸' to '触',
                '計' to '计',
                '訊' to '讯',
                '討' to '讨',
                '訓' to '训',
                '記' to '记',
                '訪' to '访',
                '設' to '设',
                '許' to '许',
                '論' to '论',
                '證' to '证',
                '識' to '识',
                '譜' to '谱',
                '變' to '变',
                '讓' to '让',
                '豐' to '丰',
                '貓' to '猫',
                '貝' to '贝',
                '負' to '负',
                '財' to '财',
                '貢' to '贡',
                '責' to '责',
                '敗' to '败',
                '貨' to '货',
                '販' to '贩',
                '貪' to '贪',
                '貴' to '贵',
                '買' to '买',
                '費' to '费',
                '賀' to '贺',
                '賊' to '贼',
                '資' to '资',
                '賦' to '赋',
                '賞' to '赏',
                '賢' to '贤',
                '賣' to '卖',
                '賭' to '赌',
                '賴' to '赖',
                '贊' to '赞',
                '趙' to '赵',
                '趕' to '赶',
                '跡' to '迹',
                '踐' to '践',
                '蹤' to '踪',
                '車' to '车',
                '軟' to '软',
                '轉' to '转',
                '輪' to '轮',
                '輕' to '轻',
                '載' to '载',
                '輝' to '辉',
                '輩' to '辈',
                '邊' to '边',
                '遙' to '遥',
                '遜' to '逊',
                '遞' to '递',
                '遠' to '远',
                '適' to '适',
                '選' to '选',
                '遺' to '遗',
                '還' to '还',
                '邏' to '逻',
                '鄭' to '郑',
                '鄰' to '邻',
                '醜' to '丑',
                '醫' to '医',
                '釋' to '释',
                '鐘' to '钟',
                '鐵' to '铁',
                '鑄' to '铸',
                '長' to '长',
                '門' to '门',
                '閃' to '闪',
                '閉' to '闭',
                '問' to '问',
                '間' to '间',
                '閣' to '阁',
                '隊' to '队',
                '陽' to '阳',
                '陰' to '阴',
                '陣' to '阵',
                '階' to '阶',
                '險' to '险',
                '隱' to '隐',
                '隨' to '随',
                '難' to '难',
                '雲' to '云',
                '電' to '电',
                '靜' to '静',
                '頂' to '顶',
                '項' to '项',
                '順' to '顺',
                '須' to '须',
                '頑' to '顽',
                '領' to '领',
                '頭' to '头',
                '顏' to '颜',
                '願' to '愿',
                '類' to '类',
                '風' to '风',
                '飛' to '飞',
                '飯' to '饭',
                '飲' to '饮',
                '餘' to '余',
                '館' to '馆',
                '馬' to '马',
                '馭' to '驭',
                '駕' to '驾',
                '駛' to '驶',
                '驅' to '驱',
                '驚' to '惊',
                '驗' to '验',
                '鬥' to '斗',
                '魚' to '鱼',
                '鳥' to '鸟',
                '鳴' to '鸣',
                '鴻' to '鸿',
                '鵬' to '鹏',
                '鷹' to '鹰',
                '麥' to '麦',
                '黃' to '黄',
                '點' to '点',
                '龍' to '龙',
            )

        return buildString(text.length) {
            text.forEach { append(map[it] ?: it) }
        }
    }

    override suspend fun getSearchMangaList(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        if (page > 1) return MangasPage(emptyList(), false)

        val originalQuery = query.trim()
        if (originalQuery.length < 2) return MangasPage(emptyList(), false)

        val q = traditionalToSimplifiedSearch(originalQuery)

        val body =
            FormBody.Builder()
                .add("k", q)
                .build()

        val response =
            client.post(
                "$baseUrl/s",
                headers,
                body,
            )

        val raw = response.use { it.body.string() }
        val root = JSONObject(raw)

        if (root.optString("code") != "200") {
            return MangasPage(emptyList(), false)
        }

        val data = root.optJSONArray("data")
            ?: return MangasPage(emptyList(), false)

        val mangas = mutableListOf<SManga>()

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            val title = item.optString("name").trim()
            val thumbnail = item.optString("imgurl").trim()

            if (id.isBlank() || title.isBlank()) continue

            mangas +=
                SManga.create().apply {
                    url = "/$id/"
                    this.title = title
                    if (thumbnail.isNotBlank()) {
                        thumbnail_url = thumbnail
                    }
                }
        }

        return MangasPage(
            mangas.distinctBy { it.url },
            false,
        )
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        val path = url.encodedPath.trim('/')

        if (path.isBlank() || path.contains(".html")) {
            return null
        }

        val response = client.get(url)
        val html = response.use { it.body.string() }
        val document = Jsoup.parse(html, url.toString())

        val title =
            document.selectFirst("h1")?.text()
                ?: document.title()
                    .substringBefore(" - ")
                    .trim()
                    .takeIf { it.isNotBlank() }
                ?: return null

        return SManga.create().apply {
            this.url = "/$path/"
            this.title = title
            thumbnail_url =
                document.selectFirst(".book-img img[data-src], .book-cover img[data-src], img[title][data-src]")
                    ?.absUrl("data-src")
                    ?.takeIf { it.isNotBlank() }
        }
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val mangaUrl = "$baseUrl${manga.url}"
        val response = client.get(mangaUrl)
        val html = response.use { it.body.string() }
        val document = Jsoup.parse(html, mangaUrl)

        val updated = SManga.create().apply {
            url = manga.url
            title =
                document.selectFirst("h1")?.text()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: manga.title
            author =
                document.select(".comic-info-detail p")
                    .asSequence()
                    .map { it.text().trim() }
                    .firstOrNull { it.startsWith("作者：") }
                    ?.removePrefix("作者：")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: manga.author
            thumbnail_url =
                document.selectFirst(".book-img img[data-src], .book-cover img[data-src], img[title][data-src]")
                    ?.absUrl("data-src")
                    ?.takeIf { it.isNotBlank() }
        }

        val slug = manga.url.trim('/')

        val visibleChapters =
            document.select(".chapterlistload a[href], .chaplist-box a[href]")
                .mapNotNull { a ->
                    val href = a.attr("href").substringBefore("?")
                    val normalizedHref =
                        if (href.startsWith("/")) {
                            href
                        } else {
                            "/$href"
                        }

                    val prefix = "/$slug/"
                    if (
                        !normalizedHref.startsWith(prefix) ||
                        !normalizedHref.endsWith(".html")
                    ) {
                        return@mapNotNull null
                    }

                    val chapterFile =
                        normalizedHref
                            .removePrefix(prefix)

                    if (
                        chapterFile.isBlank() ||
                        chapterFile.contains("/")
                    ) {
                        return@mapNotNull null
                    }

                    val name = a.text().trim()
                    if (name.isBlank()) {
                        return@mapNotNull null
                    }

                    SChapter.create().apply {
                        url = normalizedHref
                        this.name = name
                    }
                }

        val moreBody =
            FormBody.Builder()
                .add("id", slug)
                .build()

        val moreResponse =
            client.post(
                "$baseUrl/morechapter",
                headers,
                moreBody,
            )

        val moreJson =
            moreResponse.use {
                it.body.string()
            }

        val moreChapters =
            buildList {
                val root = JSONObject(moreJson)
                val data = root.optJSONArray("data") ?: JSONArray()

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val chapterId = item.optString("chapterid").trim()
                    val chapterName = item.optString("chaptername").trim()

                    if (
                        chapterId.isBlank() ||
                        chapterName.isBlank()
                    ) {
                        continue
                    }

                    add(
                        SChapter.create().apply {
                            url = "/$slug/$chapterId.html"
                            name = chapterName
                        },
                    )
                }
            }

        val parsedChapters =
            (visibleChapters + moreChapters)
                .distinctBy { it.url }

        if (parsedChapters.isEmpty()) {
            error("Yumanhua: 沒有解析到章節列表")
        }

        return SMangaUpdate(
            updated,
            parsedChapters,
        )
    }

    override suspend fun getPageList(
        chapter: SChapter,
    ): List<Page> {
        val chapterUrl = "$baseUrl${chapter.url.substringBefore("?")}"

        val response = client.get(chapterUrl)
        val html = response.use {
            it.body.string()
        }

        val document = Jsoup.parse(
            html,
            chapterUrl,
        )

        /*
         * 找到頁面內被 Dean Edwards Packer 包起來的 script
         */
        val packed = document
            .select("script:not([src])")
            .asSequence()
            .map { it.data() }
            .firstOrNull {
                it.contains("eval(function(p,a,c,k,e,d)") &&
                    it.contains("__c0rst96")
            }
            ?: error(
                "Yumanhua: 找不到 packed script",
            )

        /*
         * 解開 Dean Edwards Packer
         */
        val unpacked = unpackDeanEdwards(packed)

        /*
         * 正常解開後格式：
         *
         * var __c0rst96="xxxxx..."
         *
         * 不再只搜尋 __c0rst96，避免誤抓到原始 packed dictionary。
         */
        val payloadMarker = "var __c0rst96=\""

        val payloadMarkerPos =
            unpacked.indexOf(payloadMarker)

        if (payloadMarkerPos < 0) {
            error(
                "Yumanhua: 解包失敗，找不到 var __c0rst96",
            )
        }

        val payloadStart =
            payloadMarkerPos + payloadMarker.length

        val payloadEnd =
            unpacked.indexOf(
                '"',
                payloadStart,
            )

        if (payloadEnd < 0) {
            error(
                "Yumanhua: 找不到 __c0rst96 結尾",
            )
        }

        val payload =
            unpacked.substring(
                payloadStart,
                payloadEnd,
            )

        if (payload.length < 1000) {
            error(
                "Yumanhua: payload 異常，長度只有 ${payload.length}",
            )
        }

        /*
         * 第一層 Base64
         */
        val first =
            customBase64Decode(payload)

        if (first.isBlank()) {
            error(
                "Yumanhua: 第一層 Base64 解碼失敗",
            )
        }

        /*
         * 先依 readerContainer 的 id / data-id 找網站使用的 XOR key。
         * 若頁面索引取不到或對應 key 不符，再自動測試目前已知的 key。
         */
        val reader =
            document.selectFirst(".readerContainer")

        val keyIndex =
            reader?.id()?.toIntOrNull()
                ?: reader?.attr("data-id")?.toIntOrNull()

        val candidateKeys =
            buildList {
                keyIndex
                    ?.let { keys.getOrNull(it) }
                    ?.let { add(it) }

                keys.forEach { key ->
                    if (key !in this) {
                        add(key)
                    }
                }
            }

        var selectedKey: String? = null
        var xored: String? = null

        for (key in candidateKeys) {
            val candidate =
                buildString(first.length) {
                    for (i in first.indices) {
                        append(
                            (
                                first[i].code xor
                                    key[i % key.length].code
                                ).toChar(),
                        )
                    }
                }

            /*
             * XOR 後應為第二層 Base64，正常開頭是 Wy...
             * 再實際解一次確認內容為 JSON array，避免誤判。
             */
            if (candidate.startsWith("Wy")) {
                val decoded =
                    customBase64Decode(candidate)

                if (decoded.trimStart().startsWith("[")) {
                    selectedKey = key
                    xored = candidate
                    break
                }
            }
        }

        val finalXored =
            xored
                ?: error(
                    "Yumanhua: 找不到可用 XOR key，reader id=${reader?.id()}, data-id=${reader?.attr("data-id")}",
                )

        /*
         * selectedKey 保留作為診斷資訊；實際解碼使用 finalXored。
         */
        selectedKey
            ?: error("Yumanhua: XOR key 選擇失敗")

        /*
         * 第二層 Base64
         */
        val jsonText =
            customBase64Decode(finalXored)

        if (!jsonText.trimStart().startsWith("[")) {
            error(
                "Yumanhua: 第二層 Base64 解碼失敗，開頭=${jsonText.take(30)}",
            )
        }

        /*
         * JSON 內容就是所有漫畫圖片網址
         */
        val jsonArray =
            try {
                JSONArray(jsonText)
            } catch (e: Exception) {
                error(
                    "Yumanhua: 圖片 JSON 解析失敗：${e.message}",
                )
            }

        if (jsonArray.length() == 0) {
            error(
                "Yumanhua: 圖片列表是空的",
            )
        }

        val pages =
            mutableListOf<Page>()

        for (i in 0 until jsonArray.length()) {
            val imageUrl =
                jsonArray.optString(i)

            if (
                imageUrl.startsWith("https://") &&
                imageUrl.contains("shimolife.com")
            ) {
                pages += Page(
                    index = pages.size,
                    imageUrl = imageUrl,
                )
            }
        }

        if (pages.isEmpty()) {
            error(
                "Yumanhua: 沒有解析到有效圖片網址",
            )
        }

        return pages
    }

    /*
     * 網站使用的 Base64 解碼方式
     */
    private fun customBase64Decode(
        input: String,
    ): String {
        if (input.isEmpty()) {
            return input
        }

        val alphabet =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="

        val output =
            StringBuilder()

        var i = 0

        while (i < input.length) {
            val c1 =
                alphabet.indexOf(
                    input.getOrNull(i++) ?: '=',
                )

            val c2 =
                alphabet.indexOf(
                    input.getOrNull(i++) ?: '=',
                )

            val c3 =
                alphabet.indexOf(
                    input.getOrNull(i++) ?: '=',
                )

            val c4 =
                alphabet.indexOf(
                    input.getOrNull(i++) ?: '=',
                )

            if (c1 < 0 || c2 < 0) {
                break
            }

            val bits =
                (c1 shl 18) or
                    (c2 shl 12) or
                    (
                        (
                            if (c3 < 0) {
                                64
                            } else {
                                c3
                            }
                            ) shl 6
                        ) or
                    (
                        if (c4 < 0) {
                            64
                        } else {
                            c4
                        }
                        )

            output.append(
                (
                    (bits shr 16) and 0xff
                    ).toChar(),
            )

            if (c3 != 64 && c3 >= 0) {
                output.append(
                    (
                        (bits shr 8) and 0xff
                        ).toChar(),
                )
            }

            if (c4 != 64 && c4 >= 0) {
                output.append(
                    (
                        bits and 0xff
                        ).toChar(),
                )
            }
        }

        return output.toString()
    }

    /*
     * Dean Edwards Packer 解包
     * 不使用大型 Regex，避免之前的 StackOverflowError
     */
    private fun unpackDeanEdwards(
        script: String,
    ): String {
        val marker = "}('"

        val start =
            script.indexOf(marker)

        if (start < 0) {
            return script
        }

        var pos =
            start + marker.length

        fun readQuoted(): String? {
            val out =
                StringBuilder()

            var escaped = false

            while (pos < script.length) {
                val ch =
                    script[pos++]

                if (escaped) {
                    out.append('\\')
                    out.append(ch)
                    escaped = false
                    continue
                }

                if (ch == '\\') {
                    escaped = true
                    continue
                }

                if (ch == '\'') {
                    return out.toString()
                }

                out.append(ch)
            }

            return null
        }

        val payloadRaw =
            readQuoted()
                ?: return script

        if (
            pos >= script.length ||
            script[pos] != ','
        ) {
            return script
        }

        pos++

        val radixStart = pos

        while (
            pos < script.length &&
            script[pos].isDigit()
        ) {
            pos++
        }

        val radix =
            script.substring(
                radixStart,
                pos,
            ).toIntOrNull()
                ?: return script

        if (
            pos >= script.length ||
            script[pos] != ','
        ) {
            return script
        }

        pos++

        val countStart = pos

        while (
            pos < script.length &&
            script[pos].isDigit()
        ) {
            pos++
        }

        val count =
            script.substring(
                countStart,
                pos,
            ).toIntOrNull()
                ?: return script

        if (
            pos >= script.length ||
            script[pos] != ','
        ) {
            return script
        }

        pos++

        if (
            pos >= script.length ||
            script[pos] != '\''
        ) {
            return script
        }

        pos++

        val dictionaryRaw =
            readQuoted()
                ?: return script

        val payload =
            unescapeJs(payloadRaw)

        val dictionary =
            unescapeJs(dictionaryRaw)
                .split('|')

        val tokenMap =
            mutableMapOf<String, String>()

        for (n in 0 until count) {
            val replacement =
                dictionary
                    .getOrNull(n)
                    .orEmpty()

            if (replacement.isNotEmpty()) {
                tokenMap[
                    encodeBase(
                        n,
                        radix,
                    ),
                ] = replacement
            }
        }

        val result =
            StringBuilder(
                payload.length,
            )

        var i = 0

        while (i < payload.length) {
            val ch =
                payload[i]

            if (
                ch.isLetterOrDigit() ||
                ch == '_'
            ) {
                val tokenStart = i

                while (
                    i < payload.length &&
                    (
                        payload[i].isLetterOrDigit() ||
                            payload[i] == '_'
                        )
                ) {
                    i++
                }

                val token =
                    payload.substring(
                        tokenStart,
                        i,
                    )

                result.append(
                    tokenMap[token]
                        ?: token,
                )
            } else {
                result.append(ch)
                i++
            }
        }

        return result.toString()
    }

    private fun encodeBase(
        value: Int,
        radix: Int,
    ): String {
        if (value == 0) {
            return "0"
        }

        var n = value

        val out =
            StringBuilder()

        while (n > 0) {
            val digit =
                n % radix

            val ch =
                when {
                    digit < 36 ->
                        "0123456789abcdefghijklmnopqrstuvwxyz"[digit]

                    else ->
                        (digit + 29).toChar()
                }

            out.append(ch)
            n /= radix
        }

        return out
            .reverse()
            .toString()
    }

    private fun unescapeJs(
        value: String,
    ): String = value
        .replace(
            "\\\\",
            "\u0000",
        )
        .replace(
            "\\'",
            "'",
        )
        .replace(
            "\\n",
            "\n",
        )
        .replace(
            "\\r",
            "\r",
        )
        .replace(
            "\\t",
            "\t",
        )
        .replace(
            "\u0000",
            "\\",
        )

    companion object {
        private val keys =
            listOf(
                "smkhy258",
                "smkd95fv",
                "md496952",
                "cdcsdwq",
                "vbfsa256",
                "cawf151m",
                "cd56cvda",
                "8kihnt9",
                "dso15tlo",
                "5ko6plhy",
            )
    }
}
