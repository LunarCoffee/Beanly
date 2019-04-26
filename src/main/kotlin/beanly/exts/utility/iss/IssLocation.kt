package beanly.exts.utility.iss

import beanly.consts.GSON
import io.github.rybalkinsd.kohttp.dsl.httpGet
import io.github.rybalkinsd.kohttp.ext.url
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val mapboxAccessToken = File("src/main/resources/mapbox/mapbox_token.txt").readText()

class IssLocation {
    val image = "src/main/resources/mapbox/issMap.png"
    val statistics = GSON.fromJson(
        httpGet {
            url("https://api.wheretheiss.at/v1/satellites/25544")
        }.body()!!.string(),
        IssStatistics::class.java
    )!!

    init {
        saveImage()
    }

    private fun saveImage() {
        val args = "${statistics.longitude},${statistics.latitude},3/500x500"
        val rawImage = httpGet {
            url("https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/$args")
            param {
                "access_token" to mapboxAccessToken
            }
        }.body()!!.bytes()

        drawPoint(File(image).apply { writeBytes(rawImage) })
    }

    private fun drawPoint(file: File) {
        val image = ImageIO.read(file)
        val withDotLayer = BufferedImage(460, 460, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                drawImage(image, -20, -20, null)
                paint = Color.RED

                // Draw ISS location marker.
                font = Font(Font.SANS_SERIF, Font.BOLD, 30)
                drawString("×", width / 2 - 10, height / 2 + 10)

                // Draw ISS label.
                font = Font(Font.SANS_SERIF, Font.PLAIN, 24)
                drawString("ISS", width / 2 - 17, height / 2 + 39)

                dispose()
            }
        }
        ImageIO.write(withDotLayer, "png", file)
    }
}
