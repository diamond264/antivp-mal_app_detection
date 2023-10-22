package kr.ac.kaist.nmsl.antivp.modules.mal_app_detection

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import java.io.File
import com.jaredrummler.apkparser.ApkParser
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.MobileInferenceManager
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.ModelType
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.ZipFile
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.FileOutputStream


data class Features(
    val fileSize: Float,
    val numActivities: Float,
    val numReceivers: Float,
    val numServices: Float,
    val hammingWeight: Float,
    val distZeros: Float,
    val nestedApkCount: Float,
    val secretClassesDexCount: Float,
    val szCount: Float,
    val zipCount: Float,
    val mp3Count: Float,
    val dnsslCount: Float,
    val kkdataCount: Float,
    val appName: String,
    val iconName: String,
    val iconBitmap: Bitmap? = null
)

class MalAppDetectionModule : Module() {
    val TAG = "MalAppDetection"
//    val fileManager = FileManager.getInstance()
//    val storageDir = fileManager.getRootDirectory()
    val modelPath = "models/test_logistic.pt"
//    lateinit var module: org.pytorch.Module
    init {
        subscribeEvent(EventType.APP_DOWNLOADED)
//        Log.d(TAG, modelPath)
//        module = org.pytorch.Module.load(modelPath)
    }

    override fun name(): String {
        return "mal_app_detection"
    }

    fun ftMsg(features: Features): Bundle {
        val featureClass = features.javaClass
        val properties = featureClass.declaredFields

        val bundle = Bundle()

        for (property in properties) {
            property.isAccessible = true
            val value = property.get(features)
            if (value is Float && value > 0) {
                when (property.name) {
                    "secretClassesDexCount" -> {
                        bundle.putString("secretClassesDexMsg", "해당 APK 파일은 하나 이상의 secret-classes.dex file을 포함하고 있습니다. 해당 파일은 정상적인 앱에서는 사용되지 않은 파일이며, 스스로를 정상 파일인것처럼 은닉하고 악성행위를 위한 자원을 압축해제 하는 행위를 정의하고 있습니다. 이러한 파일들은 탐지를 피하기 위해 암호화 되어 있으며, 실행 시 'dn_ssl.so' 라이브러리를 사용하여 암호해제 후 동작합니다.")
                    }
                    "dnsslCount" -> {
                        bundle.putString("dnsslMsg", "해당 APK 파일은 악성 secret-classes.dex 파일 및 자원의 복호화를 위한 dn_ssl.so 오브젝트 라이브러리 파일을 포함하고 있습니다. 해당 라이브러리는 앱 설치 및 실행 시 불러와지며, 파일 내부 또는 AndroidManifest.xml 파일에 있는 암호화 키를 사용해 악성 파일들의 암호화를 푸는데에 사용됩니다.")
                    }
                    "nestedApkCount" -> {
                        bundle.putString("nestedApkMsg", "해당 APK 파일은 하나 이상의 숨겨진 APK 파일들이 존재합니다. 숨겨진 APK 파일들은 추가적인 기능 또는 보안 기능 업데이트를 가장하여 사용자에게 설치를 유도합니다. 이렇게 내부에 숨겨진 APK 파일들은 설치 시 사용자의 기기를 장악할 수 있도록 특정 권한들을 요구하고, 사용자의 기기를 보호하고 있는 보안 기능들을 강제 종료 및 삭제하여 무력화 시킵니다.")
                    }
                    "zipCount", "szCount" -> {
                        bundle.putString("encryptedResourceMsg", "해당 APK 파일은 하나 이상의 암호로 잠겨있는 압축파일(.zip 또는 .sz의 확장자)들이 있습니다. 이러한 압축파일들은 정상 앱으로 가장하기 위한 가짜 웹페이지 파일(.html, png, jpeg 등)과 가짜 음성파일(.mp3)을 다수 포함하고 있어 공공기관, 금융기관 또는 기업 등을 사칭하기 위해 사용됩니다.")
                    }
                    "kkdataCount" -> {
                        bundle.putString("kkdataMsg", "해당 APK 파일은 전화번호 바꿔치기 및 통화 연결음 조작을 위한 전화번호와 .mp3 파일을 매핑하는 정보를 담고 있습니다. 해당 APK 파일 설치 후 사용자의 기기가 장악된 경우, 사용자가 공공기관이나 금융기관에 확인 전화 시 kkdata.dat 파일에 미리 저장된 바꿔치기를 위한 전화번호와 이에 해당하는 기관의 .mp3 파일을 재생하여 마치 정상적인 전화를 발신 중인 것처럼 위장합니다.")
                    }
                }
            }
        }

        return bundle
    }

    override fun handleEvent(type: EventType, eventData: Bundle) {
        when (type) {
            EventType.APP_DOWNLOADED -> {
                val filePath = eventData.getStringArray("file_path")
                val fileManager = FileManager.getInstance()
                Log.d(TAG, filePath.contentToString())
                if (filePath != null) {
                    val apkFile = File(filePath[0])
                    val appName = apkFile.nameWithoutExtension
                    Log.d(TAG, appName)
                    val features = extractFeatures(fileManager, apkFile, appName)
//                    val receivers = extractReceivers(apkFile)

                    Log.d(TAG, "Extracted features: $features")
                    if (features != null) {
                        val ftBundle = Bundle().apply {
                            putFloatArray("ftArray", floatArrayOf(
                                features.fileSize,
                                features.numActivities,
                                features.numReceivers,
                                features.numServices,
                                features.hammingWeight,
                                features.distZeros,
                                features.nestedApkCount,
                                features.secretClassesDexCount,
                                features.szCount,
                                features.zipCount,
                                features.mp3Count,
                                features.dnsslCount,
                                features.kkdataCount,
                            ))
                            putString("appName", features.appName)
                            putString("iconName", features.iconName)
                        }
                        val mim = MobileInferenceManager()
                        val outBundle = mim.performPytorchInference(
                            ModelType.TEST_LOG_MODEL.value,
                            modelPath,
                            ftBundle
                        )

                        val lbl = outBundle.getInt("pl")
                        Log.d(TAG, lbl.toString())
                        Log.d(TAG, outBundle.getFloat("cf").toString())
                        if (lbl == 1) {
                            outBundle.putStringArray("phishing", arrayOf(
                                "보이스피싱앱",
                                "보이스피싱 악성앱 다운로드가 탐지되었습니다!",
                            ))
                            raiseEvent(EventType.PHISHING_APP_DETECTED, outBundle)
                            Log.d(TAG, outBundle.toString())

                            if (apkFile.exists()) {
                                apkFile.delete()
                            }

//                            val context = AntiVPApplication.getContext()

                            val ftMsgBundle = ftMsg(features)
//                            val currentMalAppCount = getMalAppCount(fileManager, "malApp/mal_app_detected_data.txt")
                            val dataToSave = JSONObject()
//                            dataToSave["malAppCount"] = currentMalAppCount + 1
                            dataToSave.put("appName", features.appName)
                            dataToSave.put("iconName", features.iconName)
//                            dataToSave["appName"] = features.appName
                            val ftMsgKeys = ftMsgBundle.keySet()
                            for (key in ftMsgKeys) {
                                val value = ftMsgBundle.get(key)
                                dataToSave.put(key, value)
                            }
                            saveJsonToFile(fileManager, dataToSave.toString())
//                            fileManager.save(fullPath, dataToSave.toString().toByteArray())
//                            saveDataToFile(context, "mal_app_detected_data.txt", dataToSave)
                        }
                    } else {
                        Log.e(TAG, "Failed to extract features")
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unexpected event type: $type")
            }
        }
    }

    private fun fileExists(rootDir: String, path: String): Boolean {
        val file = File(rootDir,path)
        return file.exists()
    }

    private fun saveJsonToFile(fileManager: FileManager, data: String) {
        val path = "malApp"
        val fileName = "malFile"
        var idx = 1
        val rootDir = fileManager.getRootDirectory()
        var fullPath: String

        do {
            fullPath = "$path/$fileName$idx.txt"
            idx++
        } while (fileExists(rootDir, fullPath))
        println(fullPath)

        val directory = File(rootDir, path)
        if (!directory.exists()) {
            directory.mkdirs()  // Create the directory if it doesn't exist
        }

        val file = File(rootDir, "$path/$fileName${idx-1}.txt")
        file.writeText(data)
    }

    fun calculateBitDistribution(hashValue: BigInteger): Map<String, Float> {
        val binaryRepresentation = hashValue.toString(2)
        val totalBits = binaryRepresentation.length
        val numOnes = binaryRepresentation.count { it == '1' }
        val numZeros = totalBits - numOnes
        val bitDistribution = mapOf(
            "num_ones" to numOnes.toFloat() / totalBits.toFloat(),
            "num_zeros" to numZeros.toFloat() / totalBits.toFloat()
        )
        return bitDistribution
    }

    fun getApkInfo(apkPath: String): Map<String, Int> {
        val apkFile = File(apkPath)
        val patterns = mapOf(
            "nestedApkCount" to "assets/.*\\.apk$",
            "secretClassesDexCount" to ".*secret-classes.*\\.dex$",
            "szCount" to ".*\\.sz$",
            "zipCount" to ".*\\.zip$",
            "mp3Count" to ".*\\.mp3$",
            "dnsslCount" to ".*libdn_ssl.*\\.so$",
            "kkdataCount" to ".*kkdata.*\\.dat$"
        ).mapValues { it.value.toRegex(RegexOption.MULTILINE) }

        val counts = mutableMapOf<String, Int>().withDefault { 0 }

        ZipFile(apkFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                patterns.forEach { (countName, pattern) ->
                    if (name.matches(pattern)) {
                        counts[countName] = counts.getValue(countName) + 1
                    }
                }
            }
        }

        return counts
    }

    fun extractFeatures(fileManager: FileManager, apkFile: File, appName: String): Features? {
        try {
            ApkParser.create(apkFile).use { apkParser ->
                val manifestXml = apkParser.getManifestXml()
                Log.d(TAG, manifestXml)
                val parserFactory = XmlPullParserFactory.newInstance()
                val parser = parserFactory.newPullParser()

                parser.setInput(StringReader(manifestXml))

                var fileSize = 0L
                var numActivities = 0
                var numReceivers = 0
                var numServices = 0
                var iconName: String? = null

                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "application" -> {
                                fileSize = apkFile.length()

                                for (i in 0 until parser.attributeCount) {
                                    val attributeName = parser.getAttributeName(i)
                                    if (attributeName == "icon" || attributeName == "android:icon") {
                                        iconName = parser.getAttributeValue(i)
                                        break
                                    }
                                }
                            }
                            "activity" -> {
                                numActivities++
                            }
                            "receiver" -> {
                                numReceivers++
                            }
                            "service" -> {
                                numServices++
                            }
                        }
                    }
                }
                Log.d(TAG, "Icon Name: $iconName")
                val rootDir = fileManager.getRootDirectory()
                val iconDir = File("$rootDir/icon")
//                val iconDir = File("icon")
                if (!iconDir.exists()) {
                    iconDir.mkdir()
                }
                Log.d(TAG, iconDir.toString())
                val iconFile = apkParser.iconFile
                Log.d(TAG, iconFile.toString())
                val outputFile = File(iconDir, "$appName.png")
                FileOutputStream(outputFile).use {
                    it.write(iconFile.data)
                }
                val iconBitmap = BitmapFactory.decodeFile(outputFile.absolutePath)

                // Compute hash
                val md = MessageDigest.getInstance("SHA-256")
                md.update(apkFile.readBytes())
                val sha256HashBytes = md.digest()
                val sha256HashBigInt = BigInteger(1, sha256HashBytes)
                val bitDistribution = calculateBitDistribution(sha256HashBigInt)
                val hammingWeight = bitDistribution["num_ones"]
                val distZeros = bitDistribution["num_zeros"]

                val counts = getApkInfo(apkFile.absolutePath)
                val nestedApkCount = counts["nestedApkCount"] ?: 0
                val secretClassesDexCount = counts["secretClassesDexCount"] ?: 0
                val szCount = counts["szCount"] ?: 0
                val zipCount = counts["zipCount"] ?: 0
                val mp3Count = counts["mp3Count"] ?: 0
                val dnsslCount = counts["dnsslCount"] ?: 0
                val kkdataCount = counts["kkdataCount"] ?: 0

                // Compute features
                val fileSizeFloat = fileSize / (1024*1024).toFloat()
                val numActivitiesFloat = numActivities.toFloat()
                val numReceiversFloat = numReceivers.toFloat()
                val numServicesFloat = numServices.toFloat()

                return iconName?.let {
                    Features(
                        fileSizeFloat,
                        numActivitiesFloat,
                        numReceiversFloat,
                        numServicesFloat,
                        hammingWeight!!.toFloat(),
                        distZeros!!.toFloat(),
                        nestedApkCount.toFloat(),
                        secretClassesDexCount.toFloat(),
                        szCount.toFloat(),
                        zipCount.toFloat(),
                        mp3Count.toFloat(),
                        dnsslCount.toFloat(),
                        kkdataCount.toFloat(),
                        appName,
                        it,
                        iconBitmap
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting features", e)
            Log.e(TAG, "Error message: ${e.message}")
        }
        return null
    }
}