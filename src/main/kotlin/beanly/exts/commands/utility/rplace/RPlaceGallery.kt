package beanly.exts.commands.utility.rplace

import beanly.consts.Emoji
import beanly.consts.TIME_FORMATTER
import framework.api.dsl.embed
import framework.api.extensions.error
import framework.api.extensions.send
import framework.api.extensions.success
import framework.core.CommandArguments
import framework.core.CommandContext
import framework.core.transformers.utility.SplitTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

class RPlaceGallery(private val canvas: RPlaceCanvas) {
    suspend fun takeSnapshot(ctx: CommandContext, args: CommandArguments) {
        val cleanName = args.get<String>(3).replace("=", "_")
        if (cleanName.length !in 1..20) {
            ctx.error(
                if (cleanName.isEmpty()) {
                    "I need a name to give the snapshot!"
                } else {
                    "I can't name a snapshot that name!"
                }
            )
            return
        }

        val filename = "$cleanName=${System.currentTimeMillis()}.png"
        val file = File("$SNAPSHOT_PATH/$filename")

        // Don't allow duplicates.
        if (cleanName in File(SNAPSHOT_PATH).walk().map { it.name.substringBefore("=") }) {
            ctx.error("A snapshot with that name already exists!")
            return
        }

        // First save the image without a grid.
        canvas.createAndSaveImage()

        val scaledImage = toBufferedImage(
            withContext(Dispatchers.IO) {
                ImageIO.read(File(RPlaceCanvas.IMAGE_PATH))
            }.getScaledInstance(300, 300, Image.SCALE_DEFAULT)
        )

        withContext(Dispatchers.IO) {
            ImageIO.write(scaledImage, "png", file)
            ctx.success("The snapshot was saved!")
        }
    }

    suspend fun deleteSnapshot(ctx: CommandContext, args: CommandArguments) {
        val name = args.get<String>(3)

        try {
            withContext(Dispatchers.IO) {
                Files
                    .newDirectoryStream(Paths.get(SNAPSHOT_PATH), "$name=*.png")
                    .first()
                    .toFile()
                    .delete()
                ctx.success("The snapshot has been deleted!")
            }
        } catch (e: NoSuchElementException) {
            ctx.error("There is no snapshot with that name!")
            return
        }
    }

    suspend fun sendGallery(ctx: CommandContext, args: CommandArguments) {
        val name = args.get<String>(3)

        withContext(Dispatchers.IO) {
            if (name.isEmpty()) {
                val snapshots = File(SNAPSHOT_PATH).walk().drop(1).joinToString("\n") {
                    val snapshotName = it.name.substringBefore("=")
                    val time = getSnapshotTime(it.name).replace(" at ", "` at `")
                    "**$snapshotName**: `$time`"
                }

                if (snapshots.isEmpty()) {
                    ctx.success("There are no canvas snapshots!")
                    return@withContext
                }

                ctx.send(
                    embed {
                        title = "${Emoji.SNOW_CAPPED_MOUNTAIN}  Canvas snapshots:"
                        description = snapshots
                    }
                )
            } else {
                val path = try {
                    Files
                        .newDirectoryStream(Paths.get(SNAPSHOT_PATH), "$name=*.png")
                        .first()
                        .toFile()
                        .path
                } catch (e: NoSuchElementException) {
                    ctx.error("There is no snapshot with that name!")
                    return@withContext
                }

                ctx.sendMessage(
                    embed {
                        title = "${Emoji.SNOW_CAPPED_MOUNTAIN}  Canvas snapshot info:"
                        description = """
                            |**Name**: $name
                            |**Time created**: ${getSnapshotTime(path)}
                        """.trimMargin()
                    }
                ).addFile(File(path)).queue()
            }
        }
    }

    private fun toBufferedImage(image: Image): BufferedImage {
        return BufferedImage(
            image.getWidth(null),
            image.getHeight(null),
            BufferedImage.TYPE_INT_ARGB
        ).apply {
            createGraphics().apply {
                drawImage(image, 0, 0, null)
                dispose()
            }
        }
    }

    private fun getSnapshotTime(filename: String): String {
        val timeMs = filename
            .substringAfterLast("=")
            .substringBefore(".")
            .toLong() - System.currentTimeMillis()
        return SplitTime(timeMs).asLocal.format(TIME_FORMATTER).drop(4)
    }

    companion object {
        private const val SNAPSHOT_PATH = "src/main/resources/rplace/snapshots"
    }
}
