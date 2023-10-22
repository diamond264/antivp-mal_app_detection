package kr.ac.kaist.nmsl.antivp.modules.model_optimization

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.util.ResourceManager
import java.lang.Thread.sleep

class ModelResourceUsageTracker(private val application: Application) {
    private val TAG = "ModelResourceUsageTracker"
    private val mResourceManager = ResourceManager(application)

    @SuppressLint("LongLogTag")
    fun trackIdleResourceUsage() {
        startTracking("")
        updateModelStatus(false, "Idle") // SUJ: dummy value inserted for modelType
        sleep(1000)
        endTracking()
    }

    @SuppressLint("LongLogTag")
    fun trackModelResourceUsage(modelType: Int, modelPath: String) {
        startTracking("")
        val modelType = ModelType.fromInt(modelType)
        updateModelStatus(true, modelPath)
        // code to run model
        if (modelType == ModelType.TEST_BERT_MODEL) {
            val input_ids = IntArray(512) { 1 }
            val attention_mask = IntArray(512) { 1 }

            // Compose all data into a Bundle
            val inputData = Bundle()
            inputData.putIntArray("input_ids", input_ids)
            inputData.putIntArray("attention_mask", attention_mask)

            // Perform inference
            val mim = MobileInferenceManager()
            val outBundle = mim.performPytorchInference(
                ModelType.TEST_BERT_MODEL.value,
                modelPath,
                inputData
            )
            val out = outBundle.getLong("out1")
            Log.d(TAG, out.toString())
        }
        else if (modelType == ModelType.TEST_S2T_MODEL) {
            val input_features = FloatArray(1*298*80) { 1.0f }
            val attention_mask = IntArray(1*298) { 1 }

            // Compose all data into a Bundle
            val inputData = Bundle()
            inputData.putFloatArray("input_features", input_features)
            inputData.putIntArray("attention_mask", attention_mask)

            // Perform inference
            val mim = MobileInferenceManager()
            val outBundle = mim.performPytorchInference(
                ModelType.TEST_S2T_MODEL.value,
                "models/test_model3.pt",
                inputData
            )
            val out = outBundle.getLongArray("out")
            Log.d(TAG, out.toString())
        }
    }

    @SuppressLint("LongLogTag")
    fun startTracking(logPath: String) {
        Log.d(TAG, "start tracking")
        mResourceManager.startMonitoring()
    }

    @SuppressLint("LongLogTag")
    fun endTracking() {
        Log.d(TAG, "end tracking")
        mResourceManager.stopMonitoring()
    }
    @SuppressLint("LongLogTag")
    fun updateModelStatus(isInferencing: Boolean, modelPath: String) {
        Log.d(TAG, "update model status")
        mResourceManager.updateModelStatus(isInferencing, modelPath)
    }
}