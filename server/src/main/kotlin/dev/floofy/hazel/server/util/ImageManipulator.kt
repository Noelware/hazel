package dev.floofy.hazel.server.util

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageManipulator {
    fun resize(stream: ByteArrayInputStream, format: String, width: Int, height: Int): ByteArray {
        val buffImage = ImageIO.read(stream)
        val resulting = buffImage.getScaledInstance(width, height, Image.SCALE_DEFAULT)
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        output.graphics.drawImage(resulting, 0, 0, null)

        val baos = ByteArrayOutputStream()
        ImageIO.write(output, format, baos)

        return baos.toByteArray()
    }

    fun format(stream: ByteArrayInputStream, to: String): ByteArray {
        val bufferedImage = ImageIO.read(stream)
        val out = ByteArrayOutputStream()

        ImageIO.write(bufferedImage, to, out)
        return out.toByteArray()
    }
}
