package com.hisham.removebg

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.hisham.removebg.ModelConstants.IMAGE_HEIGHT
import com.hisham.removebg.ModelConstants.IMAGE_WIDTH
import java.nio.FloatBuffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.pytorch.IValue
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils

/**
 * PyTorch-based class for performing image background removal by processing images with a pre-trained
 * PyTorch model. The class applies several preprocessing steps, runs the model inference, and post-processes
 * the output to generate a bitmap with the background removed.
 */
class PyTorchRemoval {

    /**
     * Runs the inference on the input bitmap, applies various preprocessing and postprocessing steps,
     * and returns the resulting bitmap with the background removed.
     *
     * @param inputBitmap The input image as a bitmap.
     * @return The bitmap with the background removed, or null if processing fails.
     */
    fun runInference(inputBitmap: Bitmap): Flow<Bitmap> = flow {
        val inputTensor = preprocessImage(inputBitmap)

        // Run inference and get the result
        val outputIValue: IValue =
            PTModelInMemoryCache.getInstance().forward(IValue.from(inputTensor))

        val outputTensor: Tensor = if (outputIValue.isTuple) {
            // Assuming the first element in the tuple is the output tensor we need
            val tupels = outputIValue.toTuple()
            val tensorList = tupels[0].toTensorList()
            tensorList[0]
        } else {
            // If the output is directly a tensor
            outputIValue.toTensor()
        }

        // Post-process the output tensor to a Bitmap
        emit(postprocessOutput(outputTensor) ?: inputBitmap)
    }

    /**
     * Preprocesses the input bitmap to convert it to a PyTorch tensor.
     * The image is scaled to the required size and normalized.
     *
     * @param bitmap The input bitmap.
     * @return A PyTorch tensor representing the image.
     */
    private fun preprocessImage(bitmap: Bitmap): Tensor {
        val inputBitmap = bitmap.scale(IMAGE_WIDTH, IMAGE_HEIGHT)

        return TensorImageUtils.bitmapToFloat32Tensor(
            inputBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB,
        )
    }

    /**
     * Post-processes the output tensor from the model to generate the final bitmap.
     *
     * @param outputTensor The output tensor from the model.
     * @return The processed bitmap with the background removed.
     */
    private fun postprocessOutput(outputTensor: Tensor): Bitmap {
        val outputData = outputTensor.dataAsFloatArray
        val outputBuffer = FloatBuffer.wrap(outputData)

        val alphaMask = createMask(outputBuffer) ?: throw Exception("Post process failed")

        return convertAlphaToBinary(alphaMask)
    }
}
