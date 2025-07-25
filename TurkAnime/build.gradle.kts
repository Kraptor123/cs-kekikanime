version = 14

cloudstream {
    authors     = listOf("keyiflerolsun, kraptor123")
    language    = "tr"
    description = "Türk Anime TV - Türkiye'nin Online Anime izleme sitesi."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Anime", "AnimeMovie")
    iconUrl = "https://www.google.com/s2/favicons?domain=www.turkanime.co&sz=%size%"
}