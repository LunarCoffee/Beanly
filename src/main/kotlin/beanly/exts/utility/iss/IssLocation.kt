package beanly.exts.utility.iss

import beanly.consts.GSON
import io.github.rybalkinsd.kohttp.dsl.async.asyncHttpGet
import io.github.rybalkinsd.kohttp.ext.url
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class IssLocation {
    val image = "src/main/resources/mapbox/iss_map.png"
    lateinit var statistics: IssStatistics

    suspend fun getStatistics() {
        statistics = GSON.fromJson(
            asyncHttpGet {
                url("https://api.wheretheiss.at/v1/satellites/25544")
            }.await().body()!!.charStream().readText(),
            IssStatistics::class.java
        )!!
    }

    suspend fun saveImage() {
        val args = "${statistics.longitude},${statistics.latitude},3/800x800"
        val rawImage = asyncHttpGet {
            url("https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/$args")
            param {
                "access_token" to mapboxToken
            }
        }.await().body()!!.byteStream()

        // If the command is used too fast, the image might be corrupted. Not sure, should probably
        // investigate further.
        drawMarkerAndLabel(File(image).apply { writeBytes(rawImage.readBytes()) })
    }

    private fun drawMarkerAndLabel(file: File) {
        val image = ImageIO.read(file)
        val withDotLayer = BufferedImage(760, 760, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                drawImage(image, -20, -20, null)
                paint = Color.RED

                // Draw ISS location marker.
                font = Font(Font.SANS_SERIF, Font.BOLD, 60)
                drawString("×", width / 2 - 20, height / 2 + 20)

                // Draw ISS label.
                font = Font(Font.SANS_SERIF, Font.PLAIN, 42)
                drawString("ISS", width / 2 - 28, height / 2 + 64)

                dispose()
            }
        }
        ImageIO.write(withDotLayer, "png", file)
    }

    companion object {
        private val mapboxToken = File("src/main/resources/mapbox/mapbox_token.txt").readText()
    }
}
