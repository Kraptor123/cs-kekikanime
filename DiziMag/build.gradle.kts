version = 18

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "En son çıkan yabancı dizi ve filmleri DiziMag farkıyla full hd 1080p kalitede izle. Dizimag Geniş ve güncel arşiviyle dizi ve filmlerin tadını çıkartın."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://dizimag.pro&size=32"
}
