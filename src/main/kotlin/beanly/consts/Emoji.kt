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
    INDICATOR_F("\uD83C\uDDEB"),
    HAMMER_AND_WRENCH("\uD83D\uDEE0️"),
    SCALES("\u2696"),
    ALARM_CLOCK("\u23F0"),
    MUTE("\uD83D\uDD07");

    override fun toString() = cp
}
