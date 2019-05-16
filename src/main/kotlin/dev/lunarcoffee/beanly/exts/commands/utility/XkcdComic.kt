package dev.lunarcoffee.beanly.exts.commands.utility

import dev.lunarcoffee.beanly.consts.GSON
import io.github.rybalkinsd.kohttp.dsl.async.asyncHttpGet
import io.github.rybalkinsd.kohttp.ext.url

class XkcdComic(
    val num: String,
    val title: String,
    val alt: String,
    val img: String,
    private val day: String,
    private val month: String,
    private val year: String
) {
    val date get() = "${day.padStart(2, '0')}/${month.padStart(2, '0')}/$year"

    companion object {
        val COMIC_404 = XkcdComic(
            "404",
            "404 Not Found",
            "(none)",
            "https://www.explainxkcd.com/wiki/images/9/92/not_found.png",
            "01",
            "04",
            "2008"
        )
    }
}

suspend fun getXkcd(which: Int?): XkcdComic {
    return if (which == 404) {
        // Accessing the API endpoint for 404 results in, well, a 404. Despite that, the comic
        // canonically exists as stated by Randall, so this is a special comic just for that.
        XkcdComic.COMIC_404
    } else {
        GSON.fromJson(
            asyncHttpGet {
                if (which != null) {
                    url("https://xkcd.com/$which/info.0.json")
                } else {
                    url("https://xkcd.com/info.0.json")
                }
            }.await().body()!!.charStream().readText(),
            XkcdComic::class.java
        )
    }
}
