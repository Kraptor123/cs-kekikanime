// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.helper.AesHelper
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

open class CizgiDuo : ExtractorApi() {
    override var name            = "CizgiDuo"
    override var mainUrl         = "https://cizgiduo.online"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val m3uLink:String?
        val extRef  = referer ?: ""
        val iSource = app.get(url, referer=extRef).text

        val bePlayer     = Regex("""bePlayer\('([^']+)',\s*'(\{[^}]+\})'\);""").find(iSource)?.groupValues ?: throw ErrorLoadingException("bePlayer not found")
        val bePlayerPass = bePlayer[1]
        val bePlayerData = bePlayer[2]
        val encrypted    = AesHelper.cryptoAESHandler(bePlayerData, bePlayerPass.toByteArray(), false)?.replace("\\", "") ?: throw ErrorLoadingException("failed to decrypt")
        Log.d("Kekik_${this.name}", "encrypted » $encrypted")

        m3uLink = Regex("""video_location":"([^"]+)""").find(encrypted)?.groupValues?.get(1)

        callback.invoke(
            ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink ?: throw ErrorLoadingException("m3u link not found"),
                referer = url,
                quality = Qualities.Unknown.value,
                isM3u8  = true
            )
        )
    }
}