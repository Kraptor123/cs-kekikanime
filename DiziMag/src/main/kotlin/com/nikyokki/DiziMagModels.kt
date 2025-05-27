package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty

data class Cipher(
    @JsonProperty("ct") val ct: String,
    @JsonProperty("iv") val iv: String,
    @JsonProperty("s") val s: String,
)

data class Schedule(
    @JsonProperty("client") val client: String,
    @JsonProperty("schedule") val schedule: List<AdSchedule>
)

data class AdSchedule(
    @JsonProperty("offset") val offset: String,
    @JsonProperty("tag") val tag: String,
    @JsonProperty("skipoffset") val skipOffset: String,
    @JsonProperty("skipmessage") val skipMessage: String
)

data class JsonData(
    @JsonProperty("schedule") val schedule: Schedule,
    @JsonProperty("adsecond") val adSecond: Int,
    @JsonProperty("bannerad") val bannerAd: List<String>,
    @JsonProperty("title") val title: String,
    @JsonProperty("description") val description: String,
    @JsonProperty("video_location") val videoLocation: String,
    @JsonProperty("images") val images: String,
    @JsonProperty("watermark") val watermark: String,
    @JsonProperty("link") val link: String,
    @JsonProperty("vast") val vast: String,
    @JsonProperty("dwlink") val dwLink: String,
    @JsonProperty("exit") val exit: Boolean,
    @JsonProperty("intro") val intro: String?,
    @JsonProperty("outro") val outro: String?,
    @JsonProperty("video_id") val videoId: Int,
    @JsonProperty("siteurl") val siteUrl: String?,
    @JsonProperty("urls") val urls: String?,
    @JsonProperty("referer") val referer: String,
    @JsonProperty("sitex") val siteX: List<String>,
    @JsonProperty("strSubtitles") val strSubtitles: List<strSubtitle>,
    @JsonProperty("dws") val dws: Boolean,
    @JsonProperty("download") val download: Boolean
)

data class strSubtitle(
    @JsonProperty("file") val file: String?,
    @JsonProperty("label") val label: String?,
    @JsonProperty("kind") val kind: String?,
    @JsonProperty("language") val language: String?,
    @JsonProperty("default") val default: String?
)

data class SearchResult(
    @JsonProperty("success") val success: Boolean?,
    @JsonProperty("theme") val theme: String?
)
