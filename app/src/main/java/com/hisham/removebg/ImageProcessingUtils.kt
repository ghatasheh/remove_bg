package com.hisham.removebg

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set
import com.hisham.removebg.ModelConstants.IMAGE_HEIGHT
import java.nio.FloatBuffer

/**
 * Converts an alpha mask into a binary (black/white) mask.
 *
 * @param inputBitmap The alpha mask bitmap.
 * @return A binary bitmap with black for background and white for foreground.
 */
internal fun convertAlphaToBinary(inputBitmap: Bitmap): Bitmap {
    val outputBitmap =
        createBitmap(inputBitmap.width, inputBitmap.height)
    for (y in 0 until inputBitmap.height) {
        for (x in 0 until inputBitmap.width) {
            val alpha = Color.alpha(inputBitmap[x, y])
            if (alpha > 0) {
                outputBitmap[x, y] = Color.WHITE
            } else {
                outputBitmap[x, y] = Color.BLACK
            }
        }
    }
    return outputBitmap
}


/**
 * Creates a grayscale alpha mask from the output tensor.
 *
 * @param outputBuffer The float buffer containing the mask data.
 * @return A grayscale bitmap representing the mask.
 */
internal fun createMask(outputBuffer: FloatBuffer): Bitmap? {
    val resizedMask =
        createBitmap(ModelConstants.IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ALPHA_8)
    for (y in 0 until IMAGE_HEIGHT) {
        for (x in 0 until IMAGE_HEIGHT) {
            val index = y * ModelConstants.IMAGE_WIDTH + x
            if (index >= outputBuffer.limit()) {
                Log.e(
                    "OutputTensor",
                    "Index $index is out of bounds for buffer limit ${outputBuffer.limit()}",
                )
                return null
            }

            val maskValue = outputBuffer.get(index)
            val grayscale = (maskValue * 255).toInt()
                .coerceIn(0, 255) // Convert mask value to grayscale (0-255)
            resizedMask[x, y] = Color.argb(grayscale, grayscale, grayscale, grayscale)
        }
    }
    return resizedMask
}

fun mergeImageWithMask(
    imageBitmap: ImageBitmap,
    mask: ImageBitmap,
): ImageBitmap {
    val input = imageBitmap.asAndroidBitmap()
    val binaryMask = mask.asAndroidBitmap()

    // Now resize the mask to match the input image size
    val resizedMaskForInputImage = binaryMask.scale(input.width, input.height)

    // Create a mutable Bitmap to draw the result with transparency
    val outputBitmap = createBitmap(input.width, input.height)

    // Iterate over each pixel in the resized mask and apply it to remove the background
    for (y in 0 until input.height) {
        for (x in 0 until input.width) {
            val maskPixel = resizedMaskForInputImage[x, y]
            val pixelColor = input[x, y]

            if (maskPixel == Color.WHITE) {
                // If the mask is white, keep the original pixel
                outputBitmap[x, y] = pixelColor
            } else {
                // If the mask is black, make the pixel transparent
                outputBitmap[x, y] = Color.TRANSPARENT
            }
        }
    }
    return outputBitmap.asImageBitmap()
}