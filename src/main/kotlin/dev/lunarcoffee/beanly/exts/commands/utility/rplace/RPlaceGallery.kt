package dev.lunarcoffee.beanly.exts.commands.utility.rplace

import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.consts.TIME_FORMATTER
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.embedPaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.CommandArguments
import dev.lunarcoffee.framework.core.CommandContext
import dev.lunarcoffee.framework.core.transformers.utility.SplitTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

        // The name of each snapshot file is formatted like "nameOfSnapshot=timeCreatedMs.png."
        val filename = "$cleanName=${System.currentTimeMillis()}.png"
        val file = File("$SNAPSHOT_PATH/$filename")

        // Don't allow duplicates.
        if (cleanName in File(SNAPSHOT_PATH).walk().map { it.name.substringBefore("=") }) {
            ctx.error("A snapshot with that name already exists!")
            return
        }

        // First save the image without a grid.
        canvas.createAndSaveImage()

        // Scale the image down to save memory.
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
            GlobalScope.launch(Dispatchers.IO) {
                // Simply deleting the image will do. We also don't need to check for the return of
                // [delete] since it only returns [false] when the file does not exist, which is
                // handled by the try/catch.
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

        GlobalScope.launch(Dispatchers.IO) {
            if (name.isEmpty()) {
                // Make a string of all of the snapshots' names and creation times.
                val snapshots = File(SNAPSHOT_PATH)
                    .walk()
                    .drop(1)
                    .map {
                        val snapshotName = it.name.substringBefore("=")
                        val time = getSnapshotTime(it.name)

                        // Time to sort by (the above [time] is a formatted string).
                        val timeMs = it.name.substringAfterLast("=").substringBefore(".").toLong()

                        Triple("**$snapshotName**: $time", time, timeMs)
                    }
                    .sortedByDescending { it.third }  // Sort by time created.
                    .chunked(16)
                    .map { pairs -> pairs.joinToString("\n") { it.first } }

                if (snapshots.count() == 0) {
                    ctx.success("There are no canvas snapshots!")
                    return@launch
                }

                ctx.send(
                    embedPaginator(ctx.event.author) {
                        for (group in snapshots) {
                            page(
                                embed {
                                    title = "${Emoji.SNOW_CAPPED_MOUNTAIN}  Canvas snapshots:"
                                    description = group
                                }
                            )
                        }
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
                    return@launch
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

    // Extracts snapshot creation time from the image's filename.
    private fun getSnapshotTime(filename: String): String {
        val timeMs = filename
            .substringAfterLast("=")
            .substringBefore(".")
            .toLong() - System.currentTimeMillis()
        return SplitTime(timeMs).localWithoutWeekday()
    }

    companion object {
        private const val SNAPSHOT_PATH = "src/main/resources/rplace/snapshots"
    }
}
