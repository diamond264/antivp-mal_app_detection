package kr.ac.kaist.nmsl.antivp.modules.speech_to_text

import android.os.Bundle
import android.os.Environment
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import kotlinx.coroutines.*
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import org.pytorch.IValue
import org.pytorch.Tensor
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpeechToTextModule() : Module() {
    private val TAG = "SpeechToText"
    val fileManager = FileManager.getInstance()
    val storageDir = fileManager.getRootDirectory()
    val modelPath = "$storageDir/models/stt.ptl"
    lateinit var module: org.pytorch.Module
    val maxTimeLimit = 160000

    init {
        subscribeEvent(EventType.CALL_OFFHOOK)
        subscribeEvent(EventType.CALL_IDLE)
        Log.d(TAG, modelPath)
        module = org.pytorch.Module.load(modelPath)
    }

    override fun name(): String {
        return "speech_to_text"
    }

    override fun handleEvent(type: EventType, bundle: Bundle) {
        when(type) {
            EventType.CALL_OFFHOOK -> {
                val dialogueId = bundle.getString("dialogue_id")
                val filename = bundle.getString("record_file")
                filename ?: return

                val outStr = transcribeFile(filename)
                dialogueId?.let { storeResult(it, outStr) }
                val outBundle = Bundle()
                outBundle.putString("dialogue_id", dialogueId)
                raiseEvent(EventType.TEXT_TRANSCRIBED, outBundle)
            }
            else -> {}
        }
    }

    fun transcribeFile(inputPath: String): String {
        val inputChunks = composeInputData(inputPath)
        var outStr = ""
        for (inputChunk in inputChunks) {
            outStr += performInference(inputChunk)
        }
        return outStr
    }

    fun performInference(inputData: Bundle): String{
        val input_features_array = inputData.getFloatArray("input_features")
        val input_features_shape = arrayOf<Long>(1, inputData.getLong("input_size")).toLongArray()
        val input_features_tensor = IValue.from(Tensor.fromBlob(input_features_array, input_features_shape))

        val output = module.forward(input_features_tensor).toStr()
        Log.d(TAG,"out: "+output.toString())
        return output.toString()
    }

    fun composeInputData(path: String): List<Bundle> {
        val filePath = "$storageDir/$path"
        val wav = FileInputStream(File(filePath))
        val bufferedAudio = BufferedInputStream(wav)

        val byteOrder = ByteOrder.LITTLE_ENDIAN
        val buffer = ByteBuffer.allocate(2)

        val arr = arrayListOf<Float>()
        buffer.order(byteOrder)

        do {
            if (bufferedAudio.read(buffer.array()) == -1){
                break
            }
            buffer.rewind() // Rewind buffer to read the data from the beginning
            val sample = buffer.getShort().toFloat()/Short.MAX_VALUE.toFloat()
            arr.add(sample)
            buffer.clear()
        } while (true)

        val inputarr = arr.drop(22)

        val input_features = inputarr.toFloatArray()
        val arrsize = inputarr.size.toLong()

        val numChunks = (arrsize + maxTimeLimit - 1) / maxTimeLimit

        val inputChunks = mutableListOf<FloatArray>()
        for (i in 0 until numChunks) {
            val startIndex = (i * maxTimeLimit).toInt()
            val endIndex = minOf((i + 1) * maxTimeLimit, arrsize).toInt()
            val chunk = input_features.copyOfRange(startIndex, endIndex)
            inputChunks.add(chunk)
        }

        return inputChunks.map { chunk ->
            val inputData = Bundle()
            inputData.putFloatArray("input_features", chunk)
            inputData.putLong("input_size", chunk.size.toLong())
            inputData
        }
//        // Compose all data into a Bundle
//        val inputData = Bundle()
//        inputData.putFloatArray("input_features", input_features)
//        inputData.putLong("input_size",arrsize)
//
//        return inputData
    }

    fun storeResult(dialogueId: String, text: String) {
        val path = "transcribed/$dialogueId.txt"
        fileManager.save(path, text)
    }
}