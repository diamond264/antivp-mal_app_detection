//package kr.ac.kaist.nmsl.antivp.modules.mal_app_detection
//
//import android.os.Bundle
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import kotlinx.coroutines.*
//import kr.ac.kaist.nmsl.antivp.core.EventType
//import kr.ac.kaist.nmsl.antivp.core.Module
//import kr.ac.kaist.nmsl.antivp.core.ModuleManager
//import kr.ac.kaist.nmsl.antivp.modules.model_optimization.MobileInferenceTest
//import org.junit.runner.RunWith
//import org.junit.Test
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//
//@RunWith(AndroidJUnit4::class)
//internal class MalAPPDetectionModuleTest {
//    @Test
//    fun testModule() {
//        val latch = CountDownLatch(1)
//
//        MainScope().launch {
//            val mModuleManager = ModuleManager(ApplicationProvider.getApplicationContext())
//
//            val MalAppDetectionModule = MalAppDetectionModule()
//            val dummySpeechToTextModule = object: Module() {
//                override fun name(): String { return "whatever" }
//                override fun handleEvent(type: EventType, bundle: Bundle) {}
//            }
//
//            mModuleManager.register(MalAppDetectionModule)
//            mModuleManager.register(dummySpeechToTextModule)
//
//            val bundle = Bundle()
//            bundle.putStringArray("file_path", arrayOf(
//                "/sdcard/Download/7dee2aa14971cfd2a0c9a3acf072c47015528fdfffce8adeaf8b732f64ff66ff.apk",
//            ))
//            dummySpeechToTextModule.raiseEvent(EventType.APP_DOWNLOADED, bundle)
////            dummyTextBasedDetectionModule.raiseEvent(EventType.PHISHING_CALL_DETECTED, bundle)
////            dummyTextBasedDetectionModule.raiseEvent(EventType.SMISHING_SMS_DETECTED, bundle)
//            //dummyTextBasedDetectionModule.raiseEvent(EventType.PHISHING_APP_DETECTED, bundle)
//        }
//        latch.await(1000, TimeUnit.MILLISECONDS)
//    }
//}