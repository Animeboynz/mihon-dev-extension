package eu.kanade.tachiyomi.extension.en.devext

import android.util.Log
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import org.json.JSONArray
import rx.Observable
import java.io.IOException

class DevExt : Madara(
    "Dev Extension",
    "https://mihon-dev.netlify.app",
    "en",
) {
    override val supportsLatest = false

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
                val json = response.body?.string()
                JSONArray(json).toString(4) // Pretty print JSON with an indentation of 4 spaces
            }
        } catch (e: IOException) {
            Log.e("Animeboynz", "Failed to fetch JSON: ${e.message}")
            null
        }
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        val mangaList = mutableListOf<SManga>()

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
