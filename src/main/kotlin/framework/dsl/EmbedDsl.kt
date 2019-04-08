package framework.dsl

import net.dv8tion.jda.api.EmbedBuilder

fun embed(init: EmbedBuilder.() -> Unit) = EmbedBuilder().apply(init).build()!!
