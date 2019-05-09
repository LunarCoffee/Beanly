package beanly.exts.commands.utility.osu.beatmap

import beanly.consts.BEANLY_CONFIG
import beanly.consts.Emoji
import beanly.consts.GSON
import beanly.exts.commands.utility.osu.OsuHasMode
import com.google.gson.GsonBuilder
import framework.api.dsl.embed
import framework.api.dsl.embedPaginator
import framework.api.extensions.error
import framework.api.extensions.send
import framework.core.CommandContext
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OsuBeatmap(private val id: String, override val mode: Int) : OsuHasMode() {
    suspend fun sendDetails(ctx: CommandContext) {
        val beatmaps = getBeatmapInfo(ctx) ?: return
        ctx.send(
            embedPaginator(ctx.event.author) {
                for (beatmap in beatmaps) {
                    page(
                        embed {
                            beatmap.run {
                                val link = "https://osu.ppy.sh/beatmapsets/$id#$modeUrl/$beatmapId"

                                title = "${Emoji.WORLD_MAP}  Info on beatmap set **$name**:"
                                description = """
                                    |**Beatmap ID**: $beatmapId
                                    |**Mode**: modeName
                                    |**Creator**: $creator
                                    |**Music artist**: $artist
                                    |**Difficulty**: $starRating★
                                    |**BPM**: $bpm
                                    |**Length**: $length
                                    |**Status**: $status
                                    |**CS/AR/HP/OD**: $cs/$ar/$hp/$od
                                    |**Maximum combo**: ${maxCombo ?: "(not applicable)"}
                                    |**Link**: $link
                                """.trimMargin()
                            }
                        }
                    )
                }
            }
        )
    }

    private suspend fun getBeatmapInfo(ctx: CommandContext): List<OsuBeatmapInfo>? {
        val beatmaps = withContext(Dispatchers.Default) {
            GSON.fromJson(
                httpPost {
                    url("https://osu.ppy.sh/api/get_beatmaps")
                    param {
                        "k" to BEANLY_CONFIG.osuToken
                        "s" to id
                        "m" to mode
                    }
                }.body()!!.charStream().readText(),
                ArrayList<Map<String, Any>>().javaClass
            ).map {
                OSU_BEATMAP_GSON.fromJson(GSON.toJson(it), OsuBeatmapInfo::class.java)
            }.sortedBy { it.starRating.toDouble() }
        }

        return if (beatmaps.isEmpty()) {
            ctx.error("I can't find a beatmap with that ID!")
            null
        } else {
            beatmaps
        }
    }

    companion object {
        private val OSU_BEATMAP_GSON = GsonBuilder()
            .setFieldNamingStrategy(OsuBeatmapStrategy())
            .create()
    }
}