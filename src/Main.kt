package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.*
import javax.imageio.ImageIO

class CarvingImage(_image: BufferedImage) {
    var image = _image
    var original = _image

    var energyMatrix = Array(image.width) { DoubleArray(image.height) }
    var sumEnergyMatrix = Array(image.width) { DoubleArray(image.height) }

    fun calcEnergy() {
        var xx: Int
        var yy: Int
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                xx = if (x == 0) 1 else if (x == image.width - 1) x - 1 else x
                val colorXPrev = Color(image.getRGB(xx - 1, y))
                val colorXNext = Color(image.getRGB(xx + 1, y))
                val deltaX = (colorXPrev.red - colorXNext.red).toDouble().pow(2) +
                        (colorXPrev.green - colorXNext.green).toDouble().pow(2) +
                        (colorXPrev.blue - colorXNext.blue).toDouble().pow(2)

                yy = if (y == 0) 1 else if (y == image.height - 1) y - 1 else y
                val colorYPrev = Color(image.getRGB(x, yy - 1))
                val colorYNext = Color(image.getRGB(x, yy + 1))
                val deltaY = (colorYPrev.red - colorYNext.red).toDouble().pow(2) +
                        (colorYPrev.green - colorYNext.green).toDouble().pow(2) +
                        (colorYPrev.blue - colorYNext.blue).toDouble().pow(2)
                energyMatrix[x][y] = sqrt(deltaX + deltaY)
            }
        }
    }

    private fun sumEnergy() {
        for (x in 0 until image.width) {
            sumEnergyMatrix[x][0] = energyMatrix[x][0]
        }

        for (y in 1 until image.height) {
            for (x in 0 until image.width) {
                val left = (x - 1).coerceIn(0, image.width - 1)
                val right = (x + 1).coerceIn(0, image.width - 1)
                val min = minOf(sumEnergyMatrix[left][y - 1], sumEnergyMatrix[x][y - 1], sumEnergyMatrix[right][y - 1])
                sumEnergyMatrix[x][y] = energyMatrix[x][y] + min
            }
        }
    }

    private fun findMinSumEnergy(): Int {
        var min = sumEnergyMatrix[0][image.height - 1]
        var xx = 0
        for (x in 0 until image.width) {
            if (sumEnergyMatrix[x][image.height - 1] < min) {
                min = sumEnergyMatrix[x][image.height - 1]
                xx = x
            }
        }
        return xx
    }

    private fun deletePixel(x: Int, y: Int) {
        for (i in x until image.width - 1) {
            image.setRGB(x, y, image.getRGB(x + 1, y))
            original.setRGB(x, y, original.getRGB(x + 1, y))
        }
    }

    fun deleteSeam() {
        sumEnergy()
        var x = findMinSumEnergy()
        for (y in image.height - 1 downTo 1) {
            deletePixel(x, 0)
            val left = (x - 1).coerceIn(0, image.width - 1)
            val right = (x + 1).coerceIn(0, image.width - 1)
            val min = minOf(sumEnergyMatrix[left][y - 1], sumEnergyMatrix[x][y - 1], sumEnergyMatrix[right][y - 1])
            x =
                if (sumEnergyMatrix[left][y - 1] == min) left else if (sumEnergyMatrix[right][y - 1] == min) right else x
        }
        deletePixel(x, 0)
        image = image.getSubimage(0, 0, image.width - 1, image.height)
        original = original.getSubimage(0, 0, original.width - 1, original.height)
    }

    fun invert() {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getRGB(x, y))
                val colorNew = Color(255 - color.red, 255 - color.green, 255 - color.blue)
                image.setRGB(x, y, colorNew.rgb)
            }
        }
    }
}

fun readImage(fileName: String): BufferedImage {
    val imageFile = File(fileName)
    return ImageIO.read(imageFile)
}


fun saveImage(imageFile: File, image: BufferedImage) {
    ImageIO.write(image, "png", imageFile)
}

fun main(args: Array<String>) {
    var inParam = ""
    var outParam = ""
    var verticalCount = 0
    var horizontalCount = 0
    val params = args.asList().chunked(2).associate { Pair(it[0], it[1]) }
    if ("-in" in params) inParam = params["-in"]!!
    if ("-out" in params) outParam = params["-out"]!!
    if ("-width" in params) verticalCount = params["-width"]!!.toInt()
    if ("-height" in params) horizontalCount = params["-height"]!!.toInt()
    var image = readImage(inParam)
    val newImage = CarvingImage(image)
    newImage.invert()
    for (i in 1..verticalCount) {
        newImage.calcEnergy()
        newImage.deleteSeam()
    }
    val transposedImage = CarvingImage(transpose(newImage.original))
    transposedImage.invert()
    for (i in 1..horizontalCount) {
        transposedImage.calcEnergy()
        transposedImage.deleteSeam()
    }

    val outputFile = File(outParam)
    saveImage(outputFile, transpose(transposedImage.original))
}

fun transpose(image: BufferedImage): BufferedImage {
    val transImage = BufferedImage(image.height, image.width, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until transImage.width) {
        for (y in 0 until transImage.height) {
            transImage.setRGB(x, y, image.getRGB(y, x))
        }
    }
    return transImage
}





