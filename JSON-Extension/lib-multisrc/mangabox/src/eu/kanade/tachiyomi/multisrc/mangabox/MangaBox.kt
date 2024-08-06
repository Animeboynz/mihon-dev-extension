package eu.kanade.tachiyomi.multisrc.mangabox

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class MangaBox(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    private val dateformat: SimpleDateFormat = SimpleDateFormat("MMM-dd-yy", Locale.ENGLISH),
) : ParsedHttpSource() {

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl)

    open val popularUrlPath = "manga_list?type=topview&category=all&state=all&page="
    open val latestUrlPath = "manga_list?type=latest&category=all&state=all&page="
    open val simpleQueryPath = "search/"

    override fun popularMangaSelector() = "div.truyen-list > div.list-truyen-item-wrap"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/$popularUrlPath$page", headers)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/$latestUrlPath$page", headers)
    }

    protected fun mangaFromElement(element: Element, urlSelector: String = "h3 a"): SManga {
        return SManga.create().apply {
            element.select(urlSelector).first()!!.let {
                url = it.attr("abs:href").substringAfter(baseUrl)
                title = it.text()
            }
            thumbnail_url = element.select("img").first()!!.attr("abs:src")
        }
    }

    override fun popularMangaFromElement(element: Element): SManga = mangaFromElement(element)

    override fun latestUpdatesFromElement(element: Element): SManga = mangaFromElement(element)

    override fun popularMangaNextPageSelector() = "div.group_page, div.group-page a:not([href]) + a:not(:contains(Last))"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank() && getAdvancedGenreFilters().isEmpty()) {
            GET("$baseUrl/$simpleQueryPath${normalizeSearchQuery(query)}?page=$page", headers)
        } else {
            val url = baseUrl.toHttpUrl().newBuilder()
            if (getAdvancedGenreFilters().isNotEmpty()) {
                url.addPathSegment("advanced_search")
                url.addQueryParameter("page", page.toString())
                url.addQueryParameter("keyw", normalizeSearchQuery(query))
                var genreInclude = ""
                var genreExclude = ""
                filters.forEach { filter ->
                    when (filter) {
                        is KeywordFilter -> filter.toUriPart()?.let { url.addQueryParameter("keyt", it) }
                        is SortFilter -> url.addQueryParameter("orby", filter.toUriPart())
                        is StatusFilter -> url.addQueryParameter("sts", filter.toUriPart())
                        else -> {}
                    }
                }
                url.addQueryParameter("g_i", genreInclude)
                url.addQueryParameter("g_e", genreExclude)
            } else {
                url.addPathSegment("manga_list")
                url.addQueryParameter("page", page.toString())
                filters.forEach { filter ->
                    when (filter) {
                        is SortFilter -> url.addQueryParameter("type", filter.toUriPart())
                        is StatusFilter -> url.addQueryParameter("state", filter.toUriPart())
                        is GenreFilter -> url.addQueryParameter("category", filter.toUriPart())
                        else -> {}
                    }
                }
            }
            GET(url.build(), headers)
        }
    }

    override fun searchMangaSelector() = ".panel_story_list .story_item"

    override fun searchMangaFromElement(element: Element) = mangaFromElement(element)

    override fun searchMangaNextPageSelector() = "a.page_select + a:not(.page_last), a.page-select + a:not(.page-last)"

    open val mangaDetailsMainSelector = "div.manga-info-top, div.panel-story-info"
    open val thumbnailSelector = "div.manga-info-pic img, span.info-image img"
    open val descriptionSelector = "div#noidungm, div#panel-story-info-description"

    override fun mangaDetailsRequest(manga: SManga): Request {
        if (manga.url.startsWith("http")) {
            return GET(manga.url, headers)
        }
        return super.mangaDetailsRequest(manga)
    }

    private fun checkForRedirectMessage(document: Document) {
        if (document.select("body").text().startsWith("REDIRECT :")) {
            throw Exception("Source URL has changed")
        }
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select(mangaDetailsMainSelector).firstOrNull()?.let { infoElement ->
                title = infoElement.select("h1, h2").first()!!.text()
                author = infoElement.select("li:contains(author) a, td:containsOwn(author) + td a").eachText().joinToString()
                status = parseStatus(infoElement.select("li:contains(status), td:containsOwn(status) + td").text())
                genre = infoElement.select("div.manga-info-top li:contains(genres)").firstOrNull()
                    ?.select("a")?.joinToString { it.text() }
                    ?: infoElement.select("td:containsOwn(genres) + td a").joinToString { it.text() }
            } ?: checkForRedirectMessage(document)
            description = document.select(descriptionSelector).firstOrNull()?.ownText()
                ?.replace("""^$title summary:?\s*""".toRegex(RegexOption.IGNORE_CASE), "")
            thumbnail_url = document.select(thumbnailSelector).firstOrNull()?.attr("abs:src")
        }
    }

    private fun parseStatus(element: String): Int = when {
        element.contains("ongoing", ignoreCase = true) -> SManga.ONGOING
        element.contains("completed", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    open val chapterListSelector = "div.chapter-list div.row, div.panel-story-chapter-list ul li"
    open val chapterUrlSelector = "a"

    override fun chapterListSelector() = chapterListSelector

    private val dateFormat by lazy {
        SimpleDateFormat("MMM dd,yy", Locale.ENGLISH)
    }

    @SuppressLint("DefaultLocale")
    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            element.select(chapterUrlSelector).first()!!.let {
                url = it.attr("abs:href").substringAfter(baseUrl)
                name = it.text()
            }
            date_upload = element.select("span").last()?.text()?.let {
                try {
                    dateFormat.parse(it)?.time
                } catch (e: ParseException) {
                    null
                }
            } ?: 0
        }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        return if (chapter.url.startsWith("http")) {
            GET(chapter.url, headers)
        } else {
            GET(baseUrl + chapter.url, headers)
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div#vungdoc img, div.container-chapter-reader img").mapIndexed { i, element ->
            Page(i, "", element.attr("abs:src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // filters
    private class StatusFilter : UriPartFilter(
        "Status",
        arrayOf(
            Pair("All", "all"),
            Pair("Completed", "completed"),
            Pair("Ongoing", "ongoing"),
        ),
    )

    private class Genre(name: String, val id: String) : Filter.TriState(name)

    private class SortFilter : UriPartFilter(
        "Sort by",
        arrayOf(
            Pair("Alphabetical", "name"),
            Pair("Rating", "rating"),
            Pair("Views", "views"),
            Pair("Last updated", "latest"),
            Pair("New", "new"),
        ),
    )

    open class AdvGenre(name: String, val id: String) : Filter.TriState(name)

    protected open fun getAdvancedGenreFilters(): List<AdvGenre> = emptyList()

    override fun getFilterList(): FilterList {
        val filters = getAdvancedGenreFilters().takeIf { it.isNotEmpty() }?.toTypedArray()
        return FilterList(
            Filter.Header("Filters are ignored if using text search."),
            Filter.Separator(),
            Filter.Header("Text search ignores genre filters."),
            StatusFilter(),
            SortFilter(),
            *(filters ?: emptyArray()),
        )
    }

    protected class KeywordFilter : Filter.Select<String>(
        "Search mode",
        arrayOf(
            "Title",
            "Author",
            "Both",
        ),
    ) {
        fun toUriPart() = when (state) {
            0 -> "title"
            1 -> "author"
            2 -> "both"
            else -> null
        }
    }

    protected class GenreFilter : UriPartFilter("Genres", getGenreFilters())

    protected abstract class UriPartFilter(
        displayName: String,
        private val vals: Array<Pair<String, String>>,
    ) : Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    companion object {
        private fun getGenreFilters() = arrayOf(
            Pair("All", "all"),
            Pair("Action", "2"),
            Pair("Adventure", "4"),
            Pair("Comedy", "6"),
            Pair("Cooking", "7"),
            Pair("Doujinshi", "9"),
            Pair("Drama", "10"),
            Pair("Ecchi", "11"),
            Pair("Fantasy", "12"),
            Pair("Gender Bender", "14"),
            Pair("Harem", "17"),
            Pair("Historical", "20"),
            Pair("Horror", "21"),
            Pair("Josei", "22"),
            Pair("Martial Arts", "25"),
            Pair("Mature", "27"),
            Pair("Mecha", "28"),
            Pair("Medical", "30"),
            Pair("Mystery", "33"),
            Pair("One Shot", "34"),
            Pair("Psychological", "40"),
            Pair("Romance", "42"),
            Pair("School Life", "43"),
            Pair("Sci-Fi", "44"),
            Pair("Seinen", "45"),
            Pair("Shoujo", "46"),
            Pair("Shoujo Ai", "47"),
            Pair("Shounen", "48"),
            Pair("Shounen Ai", "49"),
            Pair("Slice of Life", "50"),
            Pair("Smut", "52"),
            Pair("Sports", "54"),
            Pair("Supernatural", "55"),
            Pair("Tragedy", "57"),
            Pair("Webtoon", "60"),
            Pair("Yaoi", "62"),
            Pair("Yuri", "63"),
        )
    }

    private fun normalizeSearchQuery(query: String) = query.trim().replace("\\s+".toRegex(), "+")
}
