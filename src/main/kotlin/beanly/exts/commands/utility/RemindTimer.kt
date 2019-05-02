package beanly.exts.commands.utility

import java.util.*

class RemindTimer(
    val time: Date,
    val guildId: String,
    val channelId: String,
    val mention: String,
    val reason: String
)
