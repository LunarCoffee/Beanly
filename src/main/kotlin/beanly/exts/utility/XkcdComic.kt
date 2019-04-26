package beanly.exts.utility

import beanly.consts.GSON
import io.github.rybalkinsd.kohttp.dsl.httpGet
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
    fun getDate() = "${day.padStart(2, '0')}/${month.padStart(2, '0')}/$year"

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

fun getXkcd(which: Int?): XkcdComic {
    return if (which == 404) {
        // Accessing the API endpoint for 404 results in, well, a 404. Despite that, the comic
        // canonically exists as stated by Randall, so this is a special comic just for that.
        XkcdComic.COMIC_404
    } else {
        GSON.fromJson(
            httpGet {
                if (which != null) {
                    url("https://xkcd.com/$which/info.0.json")
                } else {
                    url("https://xkcd.com/info.0.json")
                }
            }.body()!!.string(),
            XkcdComic::class.java
        )
    }
}
