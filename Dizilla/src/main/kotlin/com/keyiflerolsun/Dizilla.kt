// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.*
import kotlin.coroutines.cancellation.CancellationException


private val cloudflareKiller by lazy { CloudflareKiller() }
private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request  = chain.request()
        val response = chain.proceed(request)
        val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

        if (doc.html().contains("Just a moment")) {
            return cloudflareKiller.intercept(chain)
        }

        return response
    }
}

class Dizilla : MainAPI() {
    override var mainUrl = "https://dizilla.club"
    override var name = "Dizilla"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries)


    override val mainPage = mainPageOf(
        "15" to   "Aile",
        "9"  to   "Aksiyon",
        "17" to   "Animasyon",
        "5"  to   "Bilim Kurgu",
        "2"  to   "Dram",
        "12" to   "Fantastik",
        "18" to   "Gerilim",
        "3"  to   "Gizem",
        "4"  to   "Komedi",
        "8"  to   "Korku",
        "24" to   "Macera",
        "7"  to   "Romantik",
        "26" to   "Savaş",
        "1"  to   "Suç",
        "11" to   "Western",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Decode Base64 response
        val raw = app.post(
            "${mainUrl}/api/bg/findSeries?releaseYearStart=1900&releaseYearEnd=2025&imdbPointMin=5&imdbPointMax=10&categoryIdsComma=${request.data}&countryIdsComma=&orderType=date_desc&languageId=-1&currentPage=${page}&currentPageCount=24&queryStr=&categorySlugsComma=&countryCodesComma=",
            referer = "${mainUrl}/",
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Alt-Used" to "dizilla.club",
                "Connection" to "keep-alive",
                "Host" to "dizilla.club",
            ),
            interceptor = interceptor
        ).document.text()

        val decoded = JSONObject(raw)
            .getString("response")
            .let { String(Base64.decode(it, Base64.DEFAULT)) }
        Log.d("dizillayeni", "decoded = $decoded")

        // Parse JSON tree and pick result array
        val mapper = jsonMapper()
        val rootNode = mapper.readTree(decoded)
        val itemsNode = if (rootNode.isArray) {
            rootNode
        } else {
            // Try 'items' field first, then 'result'
            rootNode.get("items")?.takeIf { it.isArray } ?: rootNode.get("result")?.takeIf { it.isArray }
            ?: error("Unexpected JSON structure: $decoded")
        }

        // Convert to list of dizillaJson
        val items: List<dizillaJson> = mapper.convertValue(
            itemsNode,
            object : com.fasterxml.jackson.core.type.TypeReference<List<dizillaJson>>() {}
        )

        // Map to SearchResponse
        val home = items.map { item ->
            val slug = item.infoslug ?: error("Missing slug for ${item.infotitle}")
            val type = when {
                slug.startsWith("dizi/") -> TvType.TvSeries
                slug.startsWith("film/") -> TvType.Movie
                else -> TvType.TvSeries
            }
            val href = fixUrlNull("${mainUrl}/$slug")
                ?: error("Invalid slug URL: $slug")

            when (type) {
                TvType.TvSeries -> newTvSeriesSearchResponse(item.infotitle, href, type) {
                    posterUrl = fixUrlNull(item.infoposter?.replace("images-macellan-online.cdn.ampproject.org/i/s/", ""))
                }
                TvType.Movie -> newMovieSearchResponse(item.infotitle, href, type) {
                    posterUrl = fixUrlNull(item.infoposter?.replace("images-macellan-online.cdn.ampproject.org/i/s/", ""))
                }
                else -> error("Unsupported type for slug=$slug")
            }
        }

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val response = app.post(
                "${mainUrl}/api/bg/searchcontent?searchterm=$query",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
                    "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
                    "Accept" to "application/json, text/plain, */*",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Sec-Fetch-Site" to "same-origin",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Dest" to "empty",
                    "Referer" to "${mainUrl}/"
                ),
                interceptor = interceptor,
                referer = "${mainUrl}/"
            )
            val responseBody = response.body.string()
            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val searchResult: SearchResult = objectMapper.readValue(responseBody)
            val decodedSearch = base64Decode(searchResult.response.toString())
            val contentJson: SearchData = objectMapper.readValue(decodedSearch)
            if (contentJson.state != true) {
                throw ErrorLoadingException("Invalid Json response")
            }
            val results = mutableListOf<SearchResponse>()
            contentJson.result?.forEach {
                val name = it.title.toString()
                val link = fixUrl(it.slug.toString())
                val posterLink = it.poster.toString()
                results.add(newTvSeriesSearchResponse(name, link, TvType.TvSeries) {
                    this.posterUrl = posterLink
                })
            }
            results
        } catch (e: Exception) {
            Log.e("Dizilla", "Search error: ${e.message}")
            emptyList()
        }
    }

    private fun toSearchResponse(ad: String, link: String, posterLink: String): SearchResponse {
        return newTvSeriesSearchResponse(
            ad,
            link,
            TvType.TvSeries,
        ) {
            this.posterUrl = posterLink
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        try {
            val response = app.get(url, interceptor = interceptor)
            val bodyString = response.body.string()
            val document = org.jsoup.Jsoup.parse(bodyString)

            val titleElement = document.selectFirst("div.poster.poster h2")
            if (titleElement == null) {
                return null
            }
            val title = titleElement.ownText()

            val posterElement = document.selectFirst("div.w-full.page-top.relative img")
            val poster = fixUrlNull(posterElement?.attr("src"))
            val yearElements = document.select("div.w-fit.min-w-fit")
            var year: Int? = null
            if (yearElements.size > 1) {
                val yearText = yearElements.getOrNull(1)
                    ?.selectFirst("span.text-sm.opacity-60")
                    ?.ownText()
                year = yearText
                    ?.split(" ")
                    ?.lastOrNull()
                    ?.toIntOrNull()
            } else {
            }

            val description = document.selectFirst("div.mt-2.text-sm")?.ownText()?.trim()

            val tagsText = document.selectFirst("div.poster.poster h3")?.ownText()
            val tags = tagsText?.split(",")?.map { it.trim() }

            val ratingText = document.selectFirst("div.flex.items-center")
                ?.selectFirst("span.text-white.text-sm")
                ?.ownText()
                ?.trim()
            val rating = ratingText.toRatingInt()

            val actorsElements = document.select("div.global-box h5")
            val actors = actorsElements.map { Actor(it.ownText()) }

            val episodeses = mutableListOf<Episode>()
            val seasonElements = document.select("div.flex.items-center.flex-wrap.gap-2.mb-4 a")
            for (sezon in seasonElements) {
                val sezonHref = fixUrl(sezon.attr("href"))
                val sezonResponse = app.get(sezonHref, interceptor = interceptor)
                val sezonBody = sezonResponse.body.string()
                val sezonDoc = org.jsoup.Jsoup.parse(sezonBody)
                val split = sezonHref.split("-")
                val season = split.getOrNull(split.size - 2)?.toIntOrNull()
                val episodesContainer = sezonDoc.select("div.episodes")
                for (bolum in episodesContainer.select("div.cursor-pointer")) {
                    val linkElements = bolum.select("a")
                    if (linkElements.isEmpty()) {
                        continue
                    }
                    val epName = linkElements.last()?.ownText() ?: continue
                    val epHref = fixUrlNull(linkElements.last()?.attr("href")) ?: continue
                    val epEpisode = bolum.selectFirst("a")?.ownText()?.trim()?.toIntOrNull()
                    val newEpisode = newEpisode(epHref) {
                        this.name = epName
                        this.season = season
                        this.episode = epEpisode
                    }
                    episodeses.add(newEpisode)
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeses) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val response = app.get(data, interceptor = interceptor)
            val bodyString = response.body.string()
            val document = Jsoup.parse(bodyString)

            val scriptElement = document.selectFirst("script#__NEXT_DATA__")
            if (scriptElement == null) {
                Log.e("Dizilla", "__NEXT_DATA__ script bulunamadı")
                return false
            }
            val script = scriptElement.data()

            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val rootNode = objectMapper.readTree(script)
            val secureDataNode = rootNode.get("props")?.get("pageProps")?.get("secureData")
            if (secureDataNode == null) {
                return false
            }

            val secureDataString = secureDataNode.toString().replace("\"", "")
            val decodedData = try {
                Base64.decode(secureDataString, Base64.DEFAULT).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                return false
            }

            val decodedJson = objectMapper.readTree(decodedData)
            val sourceNode = decodedJson.get("RelatedResults")?.get("getEpisodeSources")?.get("result")?.get(0)?.get("source_content")
            if (sourceNode == null) {
                Log.e("Dizilla", "source_content bulunamadı")
                return false
            }
            val source = sourceNode.toString().replace("\"", "").replace("\\", "")
            val iframe = fixUrlNull(Jsoup.parse(source).select("iframe").attr("src"))
            if (iframe == null) {
                Log.e("Dizilla", "Iframe URL bulunamadı")
                return false
            }

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            return true

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class dizillaJson(
    @JsonProperty("original_title")  val infotitle:   String,
    @JsonProperty("description")     val infodesc:   String,
    @JsonProperty("poster_url")      val infoposter: String?,
    @JsonProperty("used_slug")       val infoslug:   String?
)