package dev.lunarcoffee.beanly.exts.commands.utility.iss

import kotlin.math.abs

class IssStatistics(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double,
    val velocity: Double
) {
    val longitudeStr get() = if (longitude < 0) "${abs(longitude)}°W" else "$longitude°E"
    val latitudeStr get() = if (latitude < 0) "${abs(latitude)}°S" else "$latitude°N"
}
