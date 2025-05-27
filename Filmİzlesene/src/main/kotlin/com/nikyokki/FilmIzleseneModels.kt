package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty

data class IframeResponse(
    @JsonProperty("hls") val hls: Boolean,
    @JsonProperty("videoImage") val videoImage: String,
    @JsonProperty("videoSource") val videoSource: String,
    @JsonProperty("securedLink") val securedLink: String,
    @JsonProperty("downloadLinks") val downloadLinks: List<String>,
    @JsonProperty("attachmentLinks") val attachmentLinks: List<String>,
    @JsonProperty("ck") val ck: String
)