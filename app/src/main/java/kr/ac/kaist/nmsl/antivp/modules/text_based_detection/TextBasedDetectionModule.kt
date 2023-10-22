package kr.ac.kaist.nmsl.antivp.modules.text_based_detection

import android.os.Bundle
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import kr.ac.kaist.nmsl.antivp.service.initActivity
import kr.ac.kaist.nmsl.antivp.ui.MainActivity
import org.pytorch.IValue
import org.pytorch.Tensor
import java.io.File
import java.lang.Math.exp

class TextBasedDetectionModule : Module() {
    private val TAG = "TextBasedDetection"
//    private val mobileInferenceManager = MobileInferenceManager()
    private val vocabList: List<String>
    val fileManager = FileManager.getInstance()
    val storageDir = fileManager.getRootDirectory()
    val modelPath = "$storageDir/models/bert.pt"
    lateinit var module: org.pytorch.Module
    private var currentText: String = "transcribed text"
    private var currentDetectionResult: String = "phishing detection result"

    init {
        subscribeEvent(EventType.TEXT_TRANSCRIBED)
        subscribeEvent(EventType.SMS_RCVD)
        val vocabFile = File("$storageDir/models/vocab.txt")
        val vocabText = vocabFile.bufferedReader().use { it.readText() }
        vocabList = vocabText.split("\n").map { it.trim() }
        module = org.pytorch.Module.load(modelPath)
    }

    override fun name(): String {
        return "text_based_detection"
    }

    override fun handleEvent(type: EventType, bundle: Bundle) {

        when(type) {
            EventType.TEXT_TRANSCRIBED -> {
                Log.d(TAG, "Rcvd a transcribed call dialogue.")

                val dialogueId = bundle.getString("dialogue_id")
                val dialogue = dialogueId?.let { parseDialogue(it) }
                dialogue?.let {
                    Log.d(TAG, it)
                    this.currentText = it
                }

                val phishingType = dialogue?.let { detectPhishingType(it) }

                if (phishingType != null) {
                    bundle.putString("phishing_type", phishingType)
                    Log.d(TAG, "Phishing call detected")
                    this.currentDetectionResult = phishingType
                    raiseEvent(EventType.PHISHING_CALL_DETECTED, bundle)
                }
            }
            EventType.SMS_RCVD -> {
                Log.d(TAG, "Rcvd a SMS.")

                val message = bundle.getString("message_body")!!
                Log.d(TAG, message)

                val phishingType = detectPhishingType(message)

                if (phishingType != null) {
                    bundle.putString("phishing_type", phishingType)
                    Log.d(TAG, "Smishing SMS detected")
                    raiseEvent(EventType.SMISHING_SMS_DETECTED, bundle)
                }
            }
            else -> {
                Log.e(TAG, "Unexpected event type: $type")
            }
        }
    }

    fun parseDialogue(dialogueId: String): String {
        val path = "transcribed/$dialogueId.txt"
        var data = fileManager.load(path).toString()
        return data
    }

    private fun encodeSentence(sentence: String): Triple<List<String>, List<Int>, List<Int>> {
        val words = sentence.split(" ")

        val tokens = mutableListOf<String>()
        val inputIds = mutableListOf<Int>()
        val attentionMask = mutableListOf<Int>()

        for (word in words) {
            var remainingWord = "▁$word"
            while (remainingWord.isNotEmpty()) {
                var i = remainingWord.length
                while (i > 0 && !vocabList.contains(remainingWord.substring(0, i))) {
                    i -= 1
                }
                if (i == 0) {
//                    tokens.add("[UNK]")
//                    inputIds.add(vocabList.indexOf("[UNK]"))
                    break
                }

                val token = remainingWord.substring(0, i)
                val inputId = vocabList.indexOf(token)
                tokens.add(token)
                inputIds.add(inputId)
                attentionMask.add(1)
                remainingWord = remainingWord.substring(i)
            }
        }
        // Prepend [CLS]
        tokens.add(0, "[CLS]")
        inputIds.add(0, 2)
        attentionMask.add(0, 1)
        // Append [SEP]
        tokens.add("[SEP]")
        inputIds.add(3)
        attentionMask.add(1)
        // Pad tokens and inputIds to maxSeqLength
        val numPadding = 512 - tokens.size
        for (i in 0 until numPadding) {
            tokens.add("[PAD]")
            inputIds.add(1)
            attentionMask.add(0)
        }

        Log.d(TAG, "Input IDs padded: $inputIds")
        Log.d(TAG, "tokens: $tokens")

        return Triple(tokens, inputIds, attentionMask)
    }

    private fun getPhishingType(classValue: Int): String? {
        return when (classValue) {
            0 -> "non-phishing"
            1 -> "phishing"
            else -> null
        }
    }

    private fun detectPhishingType(dialogue: String): String? {
        val concatenatedSentence = dialogue
        val (tokens, inputIds, attentionMask) = encodeSentence(concatenatedSentence)

        val inputData = Bundle()
        inputData.putIntArray("input_ids", inputIds.toIntArray())
        inputData.putIntArray("attention_mask", attentionMask.toIntArray())

        val resultBundle = performInference(inputData)
        Log.d(TAG, "resultBundle: $resultBundle")

        val classValue = resultBundle.getInt("classValue")
        val confidence = resultBundle.getFloat("confidence")
        val phishingType = getPhishingType(classValue)
        Log.d(TAG, "phishingType: $phishingType")

        return phishingType
    }

    fun performInference(inputData: Bundle): Bundle {
        // 2. Generating IValue(Tensor) objects from Bundle
        val input_ids_array = inputData.getIntArray("input_ids")
        val input_ids_shape = arrayOf<Long>(1, 512).toLongArray()
        val input_ids_tensor = IValue.from(Tensor.fromBlob(input_ids_array, input_ids_shape))
        val attention_mask_array = inputData.getIntArray("attention_mask")
        val attention_mask_shape = arrayOf<Long>(1, 512).toLongArray()
        val attention_mask_tensor = IValue.from(Tensor.fromBlob(attention_mask_array, attention_mask_shape))
        Log.d(TAG, "Input IDs Tensor Shape: "+input_ids_tensor.toTensor().shape().contentToString())
        Log.d(TAG, "Attention Mask Tensor Shape: "+attention_mask_tensor.toTensor().shape().contentToString())

        // 3. Forwarding IValues to the loaded model
        val outIValue = module.forward(input_ids_tensor, attention_mask_tensor)
        var classValue = 0
        var confidence = -1f

        val bundle = Bundle()
        if (outIValue.isTuple) {
            val outTuple = outIValue.toTuple()
            val logits = outTuple[0].toTensor().dataAsFloatArray
            val probs = softmax(logits)
            classValue = argmax(probs)
            confidence = probs[classValue]
            Log.d(TAG, "classValue: $classValue")
            Log.d(TAG, "confidence: $confidence")
        }

        bundle.putInt("classValue", classValue)
        bundle.putFloat("confidence", confidence)

        return bundle
    }

    fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0.0f
        val expSum = logits.fold(0.0f) { acc, logit -> (acc + exp((logit - maxLogit).toDouble())).toFloat() }
        return logits.map { (exp((it - maxLogit).toDouble()) / expSum).toFloat() }.toFloatArray()
    }

    fun argmax(probabilities: FloatArray): Int {
        var maxIndex = 0
        var maxValue = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxValue) {
                maxIndex = i
                maxValue = probabilities[i]
            }
        }
        return maxIndex
    }

    override fun toString(): String {
        return "TextBasedDetectionModule(TAG='$TAG', vocabList=$vocabList, fileManager=$fileManager, storageDir='$storageDir', modelPath='$modelPath', module=$module)"
    }

    @JvmName("getCurrentText1")
    fun getCurrentText(): String {
        return currentText
    }

    @JvmName("getCurrentDetectionResult1")
    fun getCurrentDetectionResult(): String {
        return currentDetectionResult
    }

}