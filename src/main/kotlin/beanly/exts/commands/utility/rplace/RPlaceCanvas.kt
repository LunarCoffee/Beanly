package beanly.exts.commands.utility.rplace

import beanly.consts.COL_NAMES
import beanly.consts.DB
import beanly.consts.Emoji
import beanly.exts.commands.utility.timers.RPlaceTimer
import framework.api.dsl.embed
import framework.api.extensions.await
import framework.api.extensions.error
import framework.api.extensions.send
import framework.core.CommandArguments
import framework.core.CommandContext
import framework.core.transformers.utility.SplitTime
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import sun.awt.SunHints
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

class RPlaceCanvas {
    private val canvas = Array(CANVAS_SIZE) { Array(CANVAS_SIZE) { Color.WHITE } }

    private val canvasInfo = runBlocking {
        canvasInfoCol.run {
            if (find().toList().isEmpty()) {
                RPlaceCanvasInfo(0, mutableSetOf()).apply { insertOne(this) }
            } else {
                find().toList()[0]
            }
        }
    }
    private var totalPixelsPut = AtomicLong(canvasInfo.totalPixelsPut)
    private var totalContributors = AtomicInteger(canvasInfo.contributors.size)

    suspend fun load() {
        // Fill with white if the canvas in the DB is empty.
        if (canvasCol.find().toList().isEmpty()) {
            canvasCol.insertMany(List(CANVAS_SIZE * CANVAS_SIZE) { Color.WHITE })
        }

        // Load the canvas into the array in memory.
        val dbCanvas = canvasCol
            .find()
            .toList()
            .chunked(CANVAS_SIZE)
            .map { it.toTypedArray() }
            .toTypedArray()
        dbCanvas.copyInto(canvas)
    }

    suspend fun sendCanvas(ctx: CommandContext, grid: Boolean? = true) {
        createAndSaveImage(grid ?: false)

        // When [grid] is null, we send only the image without a grid, no embed.
        if (grid == null) {
            createAndSaveImage(false)
            ctx.sendFile(File(IMAGE_PATH)).queue()
            return
        }

        ctx.sendMessage(
            embed {
                title = "${Emoji.WHITE_SQUARE_BUTTON}  Current canvas stats:"
                description = """
                    |**Total pixels put**: $totalPixelsPut
                    |**Total contributors**: $totalContributors
                """.trimMargin()

                footer { text = "Type ..rplace colors to see all available colors." }
            }
        ).addFile(File(IMAGE_PATH)).await()
    }

    suspend fun sendColors(ctx: CommandContext) {
        ctx.send(
            embed {
                title = "${Emoji.WHITE_SQUARE_BUTTON}  Available colors:"
                description += "[red, orange, yellow, green, blue, purple/violet, white, "
                description += "grey/gray, dgrey/dgray, black, brown, pink, magenta]"
            }
        )
    }

    suspend fun putPixelContext(ctx: CommandContext, args: CommandArguments) {
        val x = args.get<Int>(1) - 1
        val y = args.get<Int>(2) - 1
        val color = when (args.get<String>(3)) {
            "red" -> Color.RED
            "orange" -> Color.decode("#FFB200")
            "yellow" -> Color.YELLOW
            "green" -> Color.GREEN
            "blue" -> Color.decode("#478DFF")
            "purple", "violet" -> Color.decode("#9E42F4")
            "white" -> Color.WHITE
            "grey", "gray" -> Color.GRAY
            "dgrey", "dgray" -> Color.DARK_GRAY
            "black" -> Color.BLACK
            "brown" -> Color.decode("#704107")
            "pink" -> Color.PINK
            "magenta" -> Color.MAGENTA
            "" -> null
            else -> Color.YELLOW
        }

        // Ensure all arguments were given (they're all optional due to the <view> operation).
        if (x < 0 || y < 0 || color == null) {
            ctx.error("You didn't provide all the arguments for the operation `put`!")
            return
        }

        canvas.apply {
            if (drawPixel(ctx, x, y, color)) {
                sendCanvas(ctx)
            }
        }
    }

    private suspend fun drawPixel(ctx: CommandContext, x: Int, y: Int, color: Color): Boolean {
        if (x !in 0 until CANVAS_SIZE || y !in 0 until CANVAS_SIZE) {
            ctx.error("Those coordinates are off the canvas!")
            return false
        }

        val user = ctx.event.author
        val timer = cooldownCol.findOne(RPlaceTimer::userId eq user.id)
        if (timer != null) {
            val timeRemaining = SplitTime(timer.time.time - System.currentTimeMillis())
            ctx.error("You can't place another pixel for `$timeRemaining`!")
            return false
        }

        canvas[y][x] = color

        // Update the total pixel count and the contributors.
        storeInfoUpdates(ctx.event.author.idLong)

        // Delete everything and replace with new canvas.
        canvasCol.drop()
        canvasCol.insertMany(canvas.flatten())

        // Draw the new point to the image and save it so we can send it.
        createAndSaveImage()

        // Create a cooldown of 15 minutes.
        val newTimer = RPlaceTimer(
            Date.from(
                LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant()
            ),
            user.id
        )
        cooldownCol.insertOne(newTimer)
        newTimer.schedule(ctx.event, cooldownCol)

        return true
    }

    private fun createAndSaveImage(grid: Boolean = false) {
        val file = File(IMAGE_PATH)
        val image = BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                // Make the text look nice.
                setRenderingHint(SunHints.KEY_ANTIALIASING, SunHints.VALUE_ANTIALIAS_ON)
                setRenderingHint(SunHints.KEY_STROKE_CONTROL, SunHints.VALUE_STROKE_PURE)

                // Fill background with white.
                fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE)

                if (grid) {
                    drawWithGrid()
                } else {
                    drawWithoutGrid()
                }
            }
        }
        ImageIO.write(image, "png", file)
    }

    private fun Graphics2D.drawWithGrid() {
        paint = Color.BLACK
        font = Font(Font.SANS_SERIF, Font.PLAIN, 16)

        for (coord in 1..CANVAS_SIZE) {
            // Draw x and y axes coordinate labels, respectively.
            drawString(coord.toString(), (if (coord < 10) 40 else 35) + coord * 30, 40)
            drawString(coord.toString(), 20, 52 + coord * 30)
        }

        // Draw x and y axes, respectively.
        drawLine(55, 55, IMAGE_SIZE - 25, 55)
        drawLine(55, 55, 55, IMAGE_SIZE - 25)

        for (xC in 0 until CANVAS_SIZE) {
            for (yC in 0 until CANVAS_SIZE) {
                paint = canvas[yC][xC]

                // Draw the individual pixel (30x30).
                fillRect(60 + xC * 30, 60 + yC * 30, 30, 30)

                // Draw the grid. This method is quite inefficient compared to drawing lines. Maybe
                // do that instead?
                paint = Color.decode("#CCCCCC")
                drawRect(60 + xC * 30, 60 + yC * 30, 30, 30)
            }
        }
        dispose()
    }

    private fun Graphics2D.drawWithoutGrid() {
        val pixelSize = IMAGE_SIZE.toDouble() / CANVAS_SIZE

        for (xC in 0 until CANVAS_SIZE) {
            for (yC in 0 until CANVAS_SIZE) {
                paint = canvas[yC][xC]
                fill(
                    Rectangle2D.Double(
                        xC * pixelSize,
                        yC * pixelSize,
                        pixelSize + 0.5,
                        pixelSize + 0.5
                    )
                )
            }
        }
    }

    private suspend fun storeInfoUpdates(id: Long) {
        canvasInfo.totalPixelsPut = totalPixelsPut.incrementAndGet()

        canvasInfo.contributors += id
        totalContributors.set(canvasInfo.contributors.size)

        canvasInfoCol.updateOne(
            RPlaceCanvasInfo::totalPixelsPut eq totalPixelsPut.get() - 1, canvasInfo
        )
    }

    companion object {
        private const val IMAGE_PATH = "src/main/resources/rplace/rplace_canvas.png"
        private const val CANVAS_SIZE = 40
        private const val IMAGE_SIZE = 30 * CANVAS_SIZE + 90

        private val canvasInfoCol = DB.getCollection<RPlaceCanvasInfo>("RPlaceCanvasInfo0")
        private val canvasCol = DB.getCollection<Color>("RPlaceCanvas0")

        private val cooldownCol = DB.getCollection<RPlaceTimer>(
            COL_NAMES[RPlaceTimer::class.simpleName]!!
        )
    }
}
