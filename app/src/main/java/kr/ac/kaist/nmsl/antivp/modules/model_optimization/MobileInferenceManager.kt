package kr.ac.kaist.nmsl.antivp.modules.model_optimization

import android.os.Bundle
import android.os.Environment
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor

class MobileInferenceManager {
    private val TAG = "MobileInference"
    val storageDir: String = Environment.getExternalStorageDirectory().absolutePath

    fun performPytorchInference(modelType: Int, modelPath: String, inputData: Bundle): Bundle {
        val modelFile = "$storageDir/$modelPath"
        var bundle = Bundle()

        when (ModelType.fromInt(modelType)) {
            ModelType.PHISHING_DETECTION -> {
                val module: Module = Module.load(modelFile)

                val input_ids_array = inputData.getIntArray("input_ids")
                val input_ids_shape = arrayOf<Long>(1, 512).toLongArray()
                val input_ids_tensor = IValue.from(Tensor.fromBlob(input_ids_array, input_ids_shape))
                val attention_mask_array = inputData.getIntArray("attention_mask")
                val attention_mask_shape = arrayOf<Long>(1, 512).toLongArray()
                val attention_mask_tensor = IValue.from(Tensor.fromBlob(attention_mask_array, attention_mask_shape))

                // 3. Forwarding IValues to the loaded model
                val outTensor = module.forward(input_ids_tensor, attention_mask_tensor).toTensor();
                val out = outTensor.dataAsFloatArray
                bundle.putFloatArray("out", out)
                return bundle
            }
            ModelType.TEST_S2T_MODEL -> {
                // 1. Loading PyTorch (torchscript) module file
                Log.d(TAG, "module path: "+modelFile)
                val module: Module = Module.load(modelFile)
                Log.d(TAG, "module loaded: "+module)

                // 2. Generating IValue(Tensor) objects from Bundle
                val input_features_array = inputData.getFloatArray("input_features")
                val input_features_shape = arrayOf<Long>(1, 298, 80).toLongArray()
                val input_features_tensor = IValue.from(Tensor.fromBlob(input_features_array, input_features_shape))
                val attention_mask_array = inputData.getIntArray("attention_mask")
                val attention_mask_shape = arrayOf<Long>(1, 298).toLongArray()
                val attention_mask_tensor = IValue.from(Tensor.fromBlob(attention_mask_array, attention_mask_shape))

                // 3. Forwarding IValues to the loaded model
                val outTensor = module.forward(input_features_tensor, attention_mask_tensor).toTensor();
                val out = outTensor.dataAsLongArray

                bundle.putLongArray("out", out)
                return bundle
            }
            ModelType.TEST_LOG_MODEL -> {
                // 1. Loading PyTorch (torchscript) module file
//                Log.d(TAG, "module path: "+modelFile)
                val module: Module = Module.load(modelFile)
//                Log.d(TAG, "module loaded: "+module)

                // 2. Generating IValue(Tensor) objects from Bundle
//                val input_AD_array = inputData.getDoubleArray("ftArray").map{it.toFloat()}.toFloatArray()
                val input_AD_array = inputData.getFloatArray("ftArray")
                val input_apk_name = inputData.getString("appName")
                val num_rows = (input_AD_array?.size ?: 0) / 13
                val input_AD_shape = longArrayOf(num_rows.toLong(), 13)
                val input_AD_tensor = IValue.from(Tensor.fromBlob(input_AD_array, input_AD_shape))
                val outputTuple = module.forward(input_AD_tensor)
                val logitsTensor = outputTuple.toTuple()[0].toTensor()
                val probabilitiesTensor = outputTuple.toTuple()[1].toTensor()
                val logitsArray = logitsTensor.dataAsFloatArray
                val probabilitiesArray = probabilitiesTensor.dataAsFloatArray
                val threshold = 0.5
                val predictedLabels = probabilitiesArray.map { if (it >= threshold) 1 else 0 }
                bundle.putFloat("logit", logitsArray[0])
                bundle.putFloat("cf", probabilitiesArray[0])
                bundle.putInt("pl", predictedLabels[0])
                bundle.putString("appName", input_apk_name)

                //Log.d(TAG, "logits: ${bundle.getFloatArray("logits").contentToString()}")
                //Log.d(TAG, "probabilities: ${bundle.getFloatArray("probabilities").contentToString()}")
                //Log.d(TAG, "predictedLabels: ${bundle.getIntArray("predictedLabels").contentToString()}")

                return bundle
            }
            ModelType.SPEECH_TO_TEXT -> {
                val module: Module = Module.load(modelFile)

                val input_features_array = inputData.getFloatArray("input_features")
                val input_features_shape = arrayOf<Long>(1, inputData.getLong("input_size")).toLongArray()
                val input_features_tensor = IValue.from(Tensor.fromBlob(input_features_array, input_features_shape))

                val output = module.forward(input_features_tensor).toStr()
                bundle.putString("out", output.toString())
                return bundle
            }
            else -> {}
        }
        return bundle
    }
}