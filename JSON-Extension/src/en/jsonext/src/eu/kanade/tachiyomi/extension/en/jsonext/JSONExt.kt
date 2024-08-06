package eu.kanade.tachiyomi.extension.en.jsonext

import android.util.Log
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.io.IOException

class JSONExt : ParsedHttpSource() {
    override val name = "JSON Extension"
    final override val lang = "en"
    override val baseUrl = "https://mihon-dev.netlify.app"
    override val supportsLatest = false
    override fun chapterFromElement(element: Element): SChapter {
        TODO("Not yet implemented")
    }

    override fun chapterListSelector(): String {
        TODO("Not yet implemented")
    }

    private val data = fetchJson("https://mihon-dev.netlify.app/mangas.json")

    data class JsonManga(
        val title: String,
        val chapterUrl: String,
        val coverImage: String,
        val author: String,
        val artist: String,
        val genres: List<String>,
        val description: String,
        val chapters: List<JsonChapter>,
    )

    data class JsonChapter(
        val chapterUrl: String,
        val displayName: String,
        val chapterNumber: Int,
        val pageCount: Int,
    )

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> {
        return fetchPopularManga(page)
    }

    override fun imageUrlParse(document: Document): String {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesSelector(): String {
        TODO("Not yet implemented")
    }

    override fun mangaDetailsParse(document: Document): SManga {
        TODO("Not yet implemented")
    }

    override fun pageListParse(document: Document): List<Page> {
        TODO("Not yet implemented")
    }

    override fun popularMangaFromElement(element: Element): SManga {
        TODO("Not yet implemented")
    }

    override fun popularMangaNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun popularMangaRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun popularMangaSelector(): String {
        TODO("Not yet implemented")
    }

    override fun searchMangaFromElement(element: Element): SManga {
        TODO("Not yet implemented")
    }

    override fun searchMangaNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        TODO("Not yet implemented")
    }

    override fun searchMangaSelector(): String {
        TODO("Not yet implemented")
    }

    private fun fetchJson(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Animeboynz", "Failed to fetch JSON: ${response.message}")
                null
            } else {
                response.body?.string() // Pretty print JSON with an indentation of 4 spaces
            }
        } catch (e: IOException) {
            Log.e("Animeboynz", "Failed to fetch JSON: ${e.message}")
            null
        }
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        val mangaList = mutableListOf<SManga>()
        // ////////
        if (data == null) {
            Log.e("JSONExt", "Data is null, cannot fetch popular manga")
            return Observable.just(MangasPage(emptyList(), false))
        }
        // ////////

        try {
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val mangaObject = jsonArray.getJSONObject(i)
                val manga = SManga.create().apply {
                    url = "/${mangaObject.getString("chapterUrl")}"
                    title = mangaObject.getString("title")
                    thumbnail_url = "$baseUrl/${mangaObject.getString("coverImage")}"
                    description = mangaObject.getString("description")
                    author = mangaObject.getString("author")
                    artist = mangaObject.getString("artist")
                    status = 2
                }
                mangaList.add(manga)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Observable.just(mangaList.let { MangasPage(it, false) })
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chaptersList = mutableListOf<SChapter>()

        // ////////
        if (data == null) {
            Log.e("JSONExt", "Data is null, cannot fetch chapter list")
            return Observable.just(emptyList())
        }
        // /////////

        try {
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val mangaObject = jsonArray.getJSONObject(i)
                val mangaUrl = mangaObject.getString("chapterUrl")

                if ("/$mangaUrl" == manga.url) {
                    val chaptersArray = mangaObject.getJSONArray("chapters")
                    for (j in 0 until chaptersArray.length()) {
                        val chapterObject = chaptersArray.getJSONObject(j)
                        val chapter = SChapter.create().apply {
                            name = chapterObject.getString("displayName")
                            url = "$baseUrl/${chapterObject.getString("chapterUrl")}"
                            chapter_number = chapterObject.getInt("chapterNumber").toFloat()
                        }
                        chaptersList.add(chapter)
                    }
                    break // Exit loop once the matching manga is found
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Observable.just(chaptersList)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val pageList = mutableListOf<Page>()

        // //////
        if (data == null) {
            Log.e("JSONExt", "Data is null, cannot fetch page list")
            return Observable.just(emptyList())
        }
        // //////

        try {
            val jsonArray = JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val mangaObject = jsonArray.getJSONObject(i)
                val chaptersArray = mangaObject.getJSONArray("chapters")

                for (j in 0 until chaptersArray.length()) {
                    val chapterObject = chaptersArray.getJSONObject(j)
                    val chapterUrl = "$baseUrl/${chapterObject.getString("chapterUrl")}"

                    if (chapter.url == chapterUrl) {
                        val pageCount = chapterObject.getInt("pageCount")
                        for (k in 0 until pageCount) {
                            val pageUrl = "$chapterUrl/${k + 1}.png" // Assuming pages are named 1.png, 2.png, etc.
                            val page = Page(index = k, url = chapterUrl, imageUrl = pageUrl)
                            pageList.add(page)
                        }
                        break // Exit loop once the matching chapter is found
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Observable.just(pageList)
    }
}
