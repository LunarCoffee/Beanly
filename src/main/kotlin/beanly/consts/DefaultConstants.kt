package beanly.consts

import com.google.gson.Gson
import java.time.format.DateTimeFormatter

const val EMBED_COLOR = 0xFFECA8

val TIME_FORMATTER = DateTimeFormatter.ofPattern("E dd/MM/yyyy 'at' hh:mm a")!!
val GSON = Gson()
