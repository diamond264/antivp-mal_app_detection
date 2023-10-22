//package kr.ac.kaist.nmsl.antivp.modules.phishing_event_receiver
//
//import android.os.Bundle
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import kotlinx.coroutines.*
//import kr.ac.kaist.nmsl.antivp.core.EventType
//import kr.ac.kaist.nmsl.antivp.core.Module
//import kr.ac.kaist.nmsl.antivp.core.ModuleManager
//import org.junit.runner.RunWith
//import org.junit.Test
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//
//@RunWith(AndroidJUnit4::class)
//internal class PhishingEventReceiverModuleTest {
//    @Test
//    fun testModule() {
//        val latch = CountDownLatch(1)
//
//        MainScope().launch {
//            val mModuleManager = ModuleManager(ApplicationProvider.getApplicationContext())
//
//            val phishingEventReceiverModule = PhishingEventReceiverModule()
//
//            val dummyTextBasedDetectionModule = object: Module() {
//                override fun name(): String {return "text_based_detection"}
//                override fun handleEvent(type: EventType, bundle: Bundle) {}
//            }
//            mModuleManager.register(dummyTextBasedDetectionModule)
//            mModuleManager.register(phishingEventReceiverModule)
//
//            val bundle = Bundle()
//            val confidence_score = 0.98
//            val phishing_type = "suspicious_call"
//            val message = "You are receiving a suspected voice phishing phone call."
//
//            bundle.putStringArray("phishing_type", arrayOf(
//                message,
//                phishing_type,
//                confidence_score.toString(),
//            ))
//            dummyTextBasedDetectionModule.raiseEvent(EventType.PHISHING_CALL_DETECTED, bundle)
//        }
//        latch.await(1000, TimeUnit.MILLISECONDS)
//    }
//}