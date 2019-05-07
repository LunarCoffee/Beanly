package beanly.consts

import beanly.exts.commands.utility.GuildOverrides
import com.google.gson.Gson
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.time.format.DateTimeFormatter

const val EMBED_COLOR = 0xFFECA8

val TIME_FORMATTER = DateTimeFormatter.ofPattern("E dd/MM/yyyy 'at' hh:mm a")!!
val GSON = Gson()

val CLIENT = KMongo.createClient().coroutine
val DB = CLIENT.getDatabase("BeanlyMongoDB")
val GUILD_OVERRIDES = DB.getCollection<GuildOverrides>("GuildOverrides")

const val REMIND_TIMERS_COL_NAME = "ReminderTimers"
const val MUTE_TIMERS_COL_NAME = "MuteTimers"
