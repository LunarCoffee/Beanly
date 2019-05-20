package dev.lunarcoffee.beanly.exts.commands.utility.tags

import java.util.*

class Tag(
    val guildId: String,
    val authorId: String,
    val name: String,
    var content: String,
    val timeCreated: Date
)
