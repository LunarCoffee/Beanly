package beanly.exts.utility.iss

import kotlin.math.abs

class IssStatistics(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double,
    val velocity: Double
) {
    fun longitudeStr(): String {
        return if (longitude < 0) {
            "${abs(longitude)}°W"
        } else {
            "$longitude°E"
        }
    }

    fun latitudeStr(): String {
        return if (latitude < 0) {
            "${abs(latitude)}°S"
        } else {
            "$latitude°N"
        }
    }
}
