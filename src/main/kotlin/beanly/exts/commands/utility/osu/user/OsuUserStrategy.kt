package beanly.exts.commands.utility.osu.user

import com.google.gson.FieldNamingStrategy
import java.lang.reflect.Field

class OsuUserStrategy : FieldNamingStrategy {
    override fun translateName(field: Field): String {
        return when (field.name) {
            "userId" -> "user_id"
            "joinTimeRaw" -> "join_date"
            "globalRank" -> "pp_rank"
            "countryRank" -> "pp_country_rank"
            "ppRaw" -> "pp_raw"
            "playTimeSeconds" -> "total_seconds_played"
            "ssh", "ss", "sh", "s", "a" -> "count_rank_${field.name}"
            else -> field.name
        }
    }
}
