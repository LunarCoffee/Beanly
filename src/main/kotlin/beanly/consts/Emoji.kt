package beanly.consts

enum class Emoji(private val cp: String) {
    PING_PONG("\uD83C\uDFD3"),
    OPEN_FILE_FOLDER("\uD83D\uDCC2"),
    RADIO_BUTTON("\uD83D\uDD18"),
    PAGE_FACING_UP("\uD83D\uDCC4"),
    GAME_DIE("\uD83C\uDFB2"),
    THINKING("\uD83E\uDD14"),
    BILLIARD_BALL("\uD83C\uDFB1"),
    MAG_GLASS("\uD83D\uDD0D"),
    LAPTOP_COMPUTER("\uD83D\uDCBB"),
    FRAMED_PICTURE("\uD83D\uDDBC️"),
    COFFEE("\u2615"),
    SATELLITE("\uD83D\uDEF0️"),
    INDICATOR_F("\uD83C\uDDEB");

    override fun toString() = cp
}
