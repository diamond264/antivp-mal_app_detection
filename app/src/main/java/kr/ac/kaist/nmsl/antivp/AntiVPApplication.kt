package kr.ac.kaist.nmsl.antivp

//import com.google.android.material.color.DynamicColors

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.ModuleManager
import kr.ac.kaist.nmsl.antivp.core.util.DownloadManager
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import kr.ac.kaist.nmsl.antivp.core.util.ResourceManager
import kr.ac.kaist.nmsl.antivp.modules.call_event_generation.CallEventGenerationModule
import kr.ac.kaist.nmsl.antivp.modules.mal_app_detection.MalAppDetectionModule
import kr.ac.kaist.nmsl.antivp.modules.mal_app_detection.WatchDogModule
import kr.ac.kaist.nmsl.antivp.modules.mal_app_detection.WatchDogService
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.ModelOptimizationModule
import kr.ac.kaist.nmsl.antivp.modules.phishing_event_receiver.PhishingEventReceiverModule
import kr.ac.kaist.nmsl.antivp.modules.fake_voice_detection.FakeVoiceDetectionModule
import kr.ac.kaist.nmsl.antivp.modules.mal_activity_detection.MalActivityDetectionModule
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.ModelResourceUsageTracker
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.ModelType
import kr.ac.kaist.nmsl.antivp.modules.net_based_detection.NetBasedDetectionModule
import kr.ac.kaist.nmsl.antivp.modules.speech_to_text.SpeechToTextModule
import kr.ac.kaist.nmsl.antivp.modules.text_based_detection.TextBasedDetectionModule
import kr.ac.kaist.nmsl.antivp.service.initCallRecorderNotification
import java.io.File

class AntiVPApplication: Application() {
    val TAG = "AntiVPApplication"
    private val trackResource = false
    private val mModuleManager = ModuleManager(this)
    private lateinit var mResourceManager: ResourceManager
    companion object {
        const val CHANNEL_ID_PERSISTENT = "persistent"
        const val CHANNEL_ID_ALERTS = "alerts"
        private var appContext: Context? = null
        private var instance: AntiVPApplication? = null

        fun getContext(): Context {
            return appContext ?: throw IllegalAccessException("Application context not initialized")
        }

        // New function to get the ModuleManager
        fun getModuleManagerInstance(): ModuleManager {
            return instance?.mModuleManager ?: throw IllegalAccessException("Application instance not initialized")
        }
    }

    init {
        appContext = this
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        FileManager.initialize(this)
        val downloadManager = DownloadManager(this)
        downloadManager.downloadModelFiles()

        // Enable Material You colors
//        DynamicColors.applyToActivitiesIfAvailable(this)

        mModuleManager.register(CallEventGenerationModule())
//        mModuleManager.register(FakeVoiceDetectionModule())
//        mModuleManager.register(MalActivityDetectionModule())
        mModuleManager.register(MalAppDetectionModule())
//        mModuleManager.register(NetBasedDetectionModule())
        mModuleManager.register(PhishingEventReceiverModule())
//        mModuleManager.register(SpeechToTextModule())
//        mModuleManager.register(TextBasedDetectionModule())
        mModuleManager.register(ModelOptimizationModule())
        mModuleManager.register(WatchDogModule())

        if (trackResource) {
            mResourceManager = ResourceManager(this)
            mResourceManager.startMonitoring()
        }

        initCallRecorderNotification(this)
        createNotificationChannel()

        val intent = Intent(this, WatchDogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

//        val callEvGenModule = mModuleManager.getModule("call_event_generation")
//        val bundle = Bundle()
//        bundle.putString("dialogue_id", "id3")
//        bundle.putString("record_file", "models/voice_sample.wav")
//        callEvGenModule?.raiseEvent(EventType.CALL_OFFHOOK, bundle)

        val fileManager = FileManager.getInstance()
//        val path = "transcribed/id4.txt"
        var data = "네 명의 도용 사건 때문에 몇 가지 확인차 연락을 드린 건데요. " +
                "혹시 본인께서 김성호라는 사람 알고 계신가요?" +
                "한번 보여드릴게요." +
                "전라도 광주 출신이고요. 42세 남성입니다. " +
                "전혀 모르시는 분이신가요? 이걸 왜 여쭤봤냐면요 이번에 저희 지검에서 김성호 주범으로 인한 금융 범죄 사기단을 검거를 했는데요. " +
                "검거 현장에서 신용카드라든가 보안카드 통장 등을 압수했습니다. " +
                "근데 많은 분들 중에서도 지금 씨 명의로 개설된 농협과 하나은행 팀장이 같이 발견이 돼서 이렇게 먼저 연락을 드린 건데요." // txt hard copy

//        var data = "[서울중앙지검] " +
//                "명의도용사건이 접수되었습니다. " +
//                "요지: 2023년06월20일 범죄자 채포현장에거 본인명의로 된 농협, 하나은행 통장이 압수되셨습니다. " +
//                "명의가 도용되셨는지 아니면 통장양도 혐의가 있는지 저희 청으로 연락바랍니다. " +
//                "전화번호: 02-1599-6423." // sms txt hard copy

//        fileManager.save(path, data)
//        val sttModule = mModuleManager.getModule("speech_to_text")
//        val b = Bundle()
//        b.putString("dialogue_id", "id4")
//        sttModule?.raiseEvent(EventType.TEXT_TRANSCRIBED, b)


        val rootDir = fileManager.getRootDirectory()
        Log.d(TAG, rootDir)
        val path = "transcribed"
//        val path = "sms"
        val fileName = "id"
        val initialIdx = "1"
        val fullPath = getUniqueFilePath(rootDir, path, fileName, initialIdx)
        val finalID = fullPath.substringAfterLast('/').substringBeforeLast(".txt")
        Log.d(TAG, finalID)
        Log.d(TAG, fullPath)
//        fileManager.save(fullPath, data)

        //Assuming the data is loaded using b = bundle() with dialogue id,
        //and assuming the detection result is returned with confidence score, label, and dialogue,
//        val txtData = fileManager.load(fullPath) // --> to get the dialogue

//        val test_module = mModuleManager.getModule("mal_app_detection") //getting the module (this is a test)
        val b2 = Bundle() //--> assuming b2 is the resulting bundle from textdetection module
        b2.putInt("pl", 1) //--> mock label from textdetection module
        b2.putFloat("cf", 0.7889F) //--> mock confidence score from textdetection module
        b2.putStringArray("phishing", arrayOf(
            "보이스피싱",
            "보이스피싱 통화가 감지되었습니다!",
        ))

//        b2.putStringArray("phishing", arrayOf(
//            "스미싱",
//            "스미싱 문자가 감지되었습니다!",
//        ))
//        val handler = Handler(Looper.getMainLooper())
//        handler.postDelayed({
////            test_module?.raiseEvent(EventType.PHISHING_CALL_DETECTED, b2)
//        test_module?.raiseEvent(EventType.SMISHING_SMS_DETECTED, b2)
//        }, 5000)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // WatchDog Service Channel
            val watchDogChannelId = "watchdog_service"
            val watchDogChannelName = "WatchDog Service"
            val watchDogImportance = NotificationManager.IMPORTANCE_DEFAULT
            val watchDogChannel =
                NotificationChannel(watchDogChannelId, watchDogChannelName, watchDogImportance)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(watchDogChannel)
        }
    }

    private fun fileExists(rootDir: String, path: String): Boolean {
        val file = File(rootDir,path)
        return file.exists()
    }

    private fun getUniqueFilePath(rootDir: String, path: String, fileName: String, initialIdx: String): String {
        var idx = initialIdx.toInt()  // Convert string to int for incrementing
        println(idx)
        var fullPath: String

        do {
            fullPath = "$path/$fileName$idx.txt"
            idx++
        } while (fileExists(rootDir, fullPath))
        println(fullPath)
        return fullPath  // returns the path with a unique filename
    }

     fun getModuleManager(): ModuleManager {
         return mModuleManager
     }
    override fun onTerminate() {
        super.onTerminate()

        if (trackResource)
            mResourceManager.stopMonitoring()
    }
}
