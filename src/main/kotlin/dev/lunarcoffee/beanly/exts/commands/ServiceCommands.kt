package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.exts.commands.utility.getXkcd
import dev.lunarcoffee.beanly.exts.commands.utility.iss.IssLocation
import dev.lunarcoffee.beanly.exts.commands.utility.osu.beatmap.OsuBeatmap
import dev.lunarcoffee.beanly.exts.commands.utility.osu.user.OsuUser
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.extensions.await
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.TrWord
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
            |This command does osu! related stuff depending on the provided `action`.\n
            |&{Getting user info:}
            |If the action is `user`, I will get info for for the user with the provided `username`
            |or `userid`. This info includes things like their rank, accuracy, PP, and more.
            |&{Getting beatmap info:}
            |If the action is `beatmap`, I will get info of the beatmap with the provided id of
            |`beatmapid`. This info includes the creator, star rating, BPM, AR, drain, and more.
            |&{Selecting a gamemode:}
            |With the `mode` argument, you can specify what mode to get info about. It should be
            |`normal`, `taiko`, `catch`, or `mania`. If the action is `user`, I will get the user's
            |stats in your selected gamemode, and if the action is `beatmap`, I will get the
            |beatmaps of that gamemode for the set you specified.
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
