package beanly.exts.commands.utility.osu

import beanly.consts.BEANLY_CONFIG
import beanly.consts.Emoji
import com.google.gson.GsonBuilder
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OsuUser(private val usernameOrId: String, private val mode: Int) {
    lateinit var info: OsuUserInfo

    suspend fun getUser() {
        info = withContext(Dispatchers.Default) {
            OSU_USER_GSON.fromJson(
                httpPost {
                    url("https://osu.ppy.sh/api/get_user")
                    param {
                        "k" to BEANLY_CONFIG.osuToken
                        "u" to usernameOrId
                        "m" to mode
                    }
                }.body()!!.charStream().readText().drop(1).dropLast(1),
                OsuUserInfo::class.java
            )
        }
    }

    fun modeEmoji(): Emoji {
        return when (mode) {
            0 -> Emoji.COMPUTER_MOUSE
            1 -> Emoji.DRUM
            2 -> Emoji.PINEAPPLE
            3 -> Emoji.MUSICAL_KEYBOARD
            else -> throw IllegalStateException()
        }
    }

    companion object {
        private val OSU_USER_GSON = GsonBuilder()
            .setFieldNamingStrategy(OsuUserStrategy())
            .create()
    }
}
