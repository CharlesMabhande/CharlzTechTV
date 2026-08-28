package com.charlztech.tv.data.remote

import com.charlztech.tv.data.model.Provider

/** Local/regional channel bundles always shown in the Providers tab. */
object RegionalProviders {
    const val ASSET_PREFIX = "asset://"

    val providers: List<Provider> = listOf(
        Provider(
            id = 9001,
            title = "ZIMBABWE TV",
            image = "https://i.imgur.com/NyxeweD.png",
            catLink = "${ASSET_PREFIX}playlists/zimbabwe_tv.m3u"
        ),
        Provider(
            id = 9002,
            title = "OPENVIEW TV",
            image = "https://www.openview.co.za/wp-content/uploads/2021/06/openview-logo.png",
            catLink = "${ASSET_PREFIX}playlists/openview_tv.m3u"
        ),
        Provider(
            id = 9003,
            title = "DSTV FREE TV",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/DStv_logo.svg/320px-DStv_logo.svg.png",
            catLink = "${ASSET_PREFIX}playlists/dstv_movies.m3u"
        ),
        Provider(
            id = 9004,
            title = "MOVIE CHANNELS",
            image = "https://i.imgur.com/iSVnzR1.png",
            catLink = "${ASSET_PREFIX}playlists/movie_channels.m3u"
        ),
        Provider(
            id = 9005,
            title = "SOUTH AFRICA TV",
            image = "https://flagcdn.com/w160/za.png",
            catLink = "${ASSET_PREFIX}playlists/south_africa_tv.m3u"
        ),
        Provider(
            id = 9006,
            title = "FOX TV",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Fox_Broadcasting_Company_logo_%282019%29.svg/320px-Fox_Broadcasting_Company_logo_%282019%29.svg.png",
            catLink = "${ASSET_PREFIX}playlists/fox_tv.m3u"
        ),
        Provider(
            id = 9007,
            title = "BOTSWANA TV",
            image = "https://flagcdn.com/w160/bw.png",
            catLink = "${ASSET_PREFIX}playlists/botswana_tv.m3u"
        ),
        Provider(
            id = 9008,
            title = "ZAMBIA TV",
            image = "https://flagcdn.com/w160/zm.png",
            catLink = "${ASSET_PREFIX}playlists/zambia_tv.m3u"
        ),
        Provider(
            id = 9009,
            title = "NIGERIA TV",
            image = "https://flagcdn.com/w160/ng.png",
            catLink = "${ASSET_PREFIX}playlists/nigeria_tv.m3u"
        ),
        Provider(
            id = 9011,
            title = "NAMIBIA TV",
            image = "https://flagcdn.com/w160/na.png",
            catLink = "${ASSET_PREFIX}playlists/namibia_tv.m3u"
        ),
        Provider(
            id = 9012,
            title = "WORLD NEWS",
            image = "https://i.imgur.com/7bRVpnu.png",
            catLink = "${ASSET_PREFIX}playlists/world_news.m3u"
        ),
        Provider(
            id = 9010,
            title = "AMERICAN TV",
            image = "https://flagcdn.com/w160/us.png",
            catLink = "https://iptv-org.github.io/iptv/countries/us.m3u"
        )
    )
}
