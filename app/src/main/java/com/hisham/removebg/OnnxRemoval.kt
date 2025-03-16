package com.hisham.removebg

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.hisham.removebg.ModelConstants.IMAGE_WIDTH
import com.hisham.removebg.ModelConstants.IMAGE_HEIGHT
import com.hisham.removebg.ModelConstants.DIM_PIXEL_SIZE
import com.hisham.removebg.ModelConstants.DIM_BATCH_SIZE
import java.nio.FloatBuffer
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class OnnxRemoval {

    private val env = OrtEnvironment.getEnvironment()
    private lateinit var session: OrtSession

    fun runInference(input: Bitmap): Flow<Bitmap> = flow {
        val imgData = input.preprocess()

        // run inference
        session = createOrtSession()
        val inputName = session.inputNames?.iterator()?.next()
        val shape = longArrayOf(
            ModelConstants.DIM_BATCH_SIZE.toLong(),
            ModelConstants.DIM_PIXEL_SIZE.toLong(),
            ModelConstants.IMAGE_WIDTH.toLong(),
            ModelConstants.IMAGE_HEIGHT.toLong(),
        )

        val tensor = OnnxTensor.createTensor(env, imgData, shape)

        val result = session.run(Collections.singletonMap(inputName, tensor))

        // post process
        val outputBitmap = postProcess(result.topResultAsFloatArray())
            ?: throw Exception("Post process failed")

        // clean up
        session.close()
        env.close()
        result.close()
        tensor.close()

        emit(outputBitmap.scale(input.width, input.height),)
    }.flowOn(Dispatchers.Default)

    private suspend fun createOrtSession(): OrtSession = withContext(Dispatchers.Default) {
        env.createSession(assetFilePath(App.appContext(), MODEL_NAME))
    }

    private fun postProcess(output: FloatArray): Bitmap? {
        val buffer = FloatBuffer.wrap(output)

        val alphaMask = createMask(buffer) ?: return null

        return convertAlphaToBinary(alphaMask)
    }

    private fun OrtSession.Result.topResultAsFloatArray(): FloatArray {
        // pick first output (highest score result)
        val output = this[0].value as? Array<Array<Array<FloatArray>>>
            ?: throw Exception("Invalid output type")
        // flatten output to a single dimension array
        return output[0][0].flatMap { it.toList() }.toFloatArray()
    }

    companion object {
        private const val MODEL_NAME = "model.onnx"
    }
}

internal fun Bitmap.preprocess(
    mean: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f),
    std: FloatArray = floatArrayOf(1f, 1f, 1f),
): FloatBuffer {
    val resizedImage = this.scale(IMAGE_WIDTH, IMAGE_HEIGHT)

    val inputSize = DIM_BATCH_SIZE * DIM_PIXEL_SIZE * IMAGE_WIDTH * IMAGE_HEIGHT
    val buffer = FloatBuffer.allocate(inputSize * 4) // 4 bytes / float

    buffer.rewind()

    val stride = IMAGE_WIDTH * IMAGE_HEIGHT
    val bmpData = IntArray(stride)

    resizedImage.getPixels(
        bmpData,
        0,
        resizedImage.width,
        0,
        0,
        resizedImage.width,
        resizedImage.height,
    )

    for (i in 0..<IMAGE_WIDTH) {
        for (j in 0..<IMAGE_HEIGHT) {
            val idx = IMAGE_HEIGHT * i + j
            val pixelValue = bmpData[idx]
            buffer.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - mean[0]) / std[0]))
            buffer.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - mean[1]) / std[1]))
            buffer.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - mean[2]) / std[2]))
        }
    }

    buffer.rewind()

    return buffer
}