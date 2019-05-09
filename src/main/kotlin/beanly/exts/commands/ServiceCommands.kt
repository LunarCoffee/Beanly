package beanly.exts.commands

import beanly.consts.Emoji
import beanly.exts.commands.utility.getXkcd
import beanly.exts.commands.utility.iss.IssLocation
import beanly.exts.commands.utility.osu.beatmap.OsuBeatmap
import beanly.exts.commands.utility.osu.user.OsuUser
import beanly.trimToDescription
import framework.api.dsl.command
import framework.api.dsl.embed
import framework.api.extensions.await
import framework.api.extensions.error
import framework.api.extensions.send
import framework.core.annotations.CommandGroup
import framework.core.transformers.TrWord
import java.io.File
import kotlin.random.Random

@CommandGroup("Service")
class ServiceCommands {
    fun xkcd() = command("xkcd") {
        description = "Gets an xkcd comic!"
        aliases = listOf("getxkcd")

        extDescription = """
            |`$name [number|-r]`\n
            |Gets and displays information about the xkcd comic number `number`. If `number` is not
            |specified, the latest comic is used. If the `-r` flag is set, a random comic will be
            |used. Of course, this command also displays the comic itself, not just information.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(true))
        execute { ctx, args ->
            val whichOrRandom = args.get<String>(0)
            val latestNumber = getXkcd(null).num.toInt()

            val which = when (whichOrRandom) {
                "" -> latestNumber
                "-r" -> Random.nextInt(latestNumber) + 1
                else -> whichOrRandom.toIntOrNull()
            }

            if (which == null || which !in 1..latestNumber) {
                ctx.error("I can't get the comic with that number!")
                return@execute
            }

            val comic = getXkcd(which)
            ctx.send(
                embed {
                    comic.run {
                        this@embed.title = "${Emoji.FRAMED_PICTURE}  XKCD Comic #**$num**:"
                        description = """
                            |**Title**: $title
                            |**Alt text**: $alt
                            |**Release date**: $date
                        """.trimMargin()

                        image { url = this@run.img }
                    }
                }
            )
        }
    }

    fun iss() = command("iss") {
        description = "Shows the current location of the ISS."
        aliases = listOf("issinfo", "spacestation")

        extDescription = """
            |`$name`\n
            |Shows details about the location and other info of the International Space Station. A
            |map with a point where the ISS currently is will also be displayed. The information is
            |fetched using the `Where the ISS at?` API.
        """.trimToDescription()

        execute { ctx, _ ->
            val location = IssLocation().apply {
                getStatistics()
                saveImage()
            }

            location.run {
                ctx.sendMessage(
                    embed {
                        statistics.run {
                            title = "${Emoji.SATELLITE}  Info on the ISS:"
                            description = """
                                |**Longitude**: $longitudeStr
                                |**Latitude**: $latitudeStr
                                |**Altitude**: $altitude km
                                |**Velocity**: $velocity km/h
                            """.trimMargin()
                        }
                    }
                ).addFile(File(image)).await()
            }
        }
    }

    fun osu() = command("osu") {
        description = "Does lots of osu! related stuff!"
        aliases = listOf("ous")

        extDescription = """
            |`$name action username|userid|beatmapid [mode]`\n
            |This command does osu! related stuff depending on the provided `action`. If it is
            |`user`, it gets info for for the user with the provided `username` or `userid`. If
            |`mode` is provided, it should be `normal`, `taiko`, `catch`, or `mania`. If `action`
            |is `beatmap`, it gets info of the beatmap with the provided id of `beatmapid`.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(), TrWord(), TrWord(true))
        execute { ctx, args ->
            val action = args.get<String>(0)
            val userOrBeatmap = args.get<String>(1)
            val mode = when (args.get<String>(2).toLowerCase()) {
                "", "normal" -> 0
                "taiko" -> 1
                "catch" -> 2
                "mania" -> 3
                else -> {
                    ctx.error("That isn't a valid gamemode!")
                    return@execute
                }
            }

            when (action) {
                "user" -> OsuUser(userOrBeatmap, mode).sendDetails(ctx)
                "beatmap" -> OsuBeatmap(userOrBeatmap, mode).sendDetails(ctx)
                else -> {
                    ctx.error("That operation is invalid!")
                }
            }
        }
    }
}
