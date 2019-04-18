package beanly.exts.utility

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
