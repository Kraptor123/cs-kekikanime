// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty

open class VideoSeyred : ExtractorApi() {
    override val name            = "VideoSeyred"
    override val mainUrl         = "https://videoseyred.in"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val ext_ref   = referer ?: ""
        val video_id  = url.substringAfter("embed/").substringBefore("?")
        val video_url = "${mainUrl}/playlist/${video_id}.json"
        Log.d("Kekik_${this.name}", "video_url » ${video_url}")

        val response  = app.get(video_url).parsedSafe<List<VideoSeyredSource>>()?.firstOrNull() ?: throw Error("Failed to parse response")
        Log.d("Kekik_${this.name}", "response » ${response}")

        for (track in response.tracks) {
            if (track.label != null && track.kind == "captions") {
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = track.label,
                        url  = fixUrl(track.file)
                    )
                )
            }
        }

        for (source in response.sources) {
            callback.invoke(
                ExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = source.file,
                    referer = ext_ref,
                    quality = Qualities.Unknown.value,
                    type    = INFER_TYPE
                )
            )
        }
    }

    data class VideoSeyredSource(
        @JsonProperty("image")   val image: String,
        @JsonProperty("title")   val title: String,
        @JsonProperty("sources") val sources: List<VSSource>,
        @JsonProperty("tracks")  val tracks: List<VSTrack>
    )

    data class VSSource(
        @JsonProperty("file")    val file: String,
        @JsonProperty("type")    val type: String,
        @JsonProperty("default") val default: String
    )

    data class VSTrack(
        @JsonProperty("file")     val file: String,
        @JsonProperty("kind")     val kind: String,
        @JsonProperty("language") val language: String? = null,
        @JsonProperty("label")    val label: String?    = null,
        @JsonProperty("default")  val default: String?  = null
    )
}