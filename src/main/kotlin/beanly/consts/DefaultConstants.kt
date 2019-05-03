package beanly.consts

import com.google.gson.Gson
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.time.format.DateTimeFormatter

const val EMBED_COLOR = 0xFFECA8

val TIME_FORMATTER = DateTimeFormatter.ofPattern("E dd/MM/yyyy 'at' hh:mm a")!!
val GSON = Gson()

val CLIENT = KMongo.createClient().coroutine
val DB = CLIENT.getDatabase("BeanlyMongoDB")

const val REMIND_TIMERS_COL_NAME = "RemindTimers"
const val MUTE_TIMERS_COL_NAME = "MuteTimers"
const val NO_PAY_RESPECTS_COL_NAME = "NoPayRespects"
