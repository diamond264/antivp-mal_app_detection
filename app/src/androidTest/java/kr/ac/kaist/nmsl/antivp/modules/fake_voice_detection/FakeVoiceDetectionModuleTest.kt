package kr.ac.kaist.nmsl.antivp.modules.fake_voice_detection

import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import kr.ac.kaist.nmsl.antivp.core.ModuleManager
import org.junit.runner.RunWith
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class FakeVoiceDetectionModuleTest {
    @Test
    fun testModule() {
        val latch = CountDownLatch(1)

        MainScope().launch {
            val mModuleManager = ModuleManager()

            val fakeVoiceDetectionModule = FakeVoiceDetectionModule()

            val dummyCallEventGenerationModule = object: Module() {
                override fun name(): String { return "call_event_generation" }
                override fun handleEvent(type: EventType, bundle: Bundle) {}
            }

            mModuleManager.register(dummyCallEventGenerationModule)
            mModuleManager.register(fakeVoiceDetectionModule)

            /* Example call file Uri */
            val exampleCallFile = Uri.parse("android.resource://kr.ac.kaist.nmsl.antivp/raw/test_audio")

            val bundle = Bundle()
            bundle.putBoolean("success", true)
            bundle.putString("record_file", exampleCallFile.toString())
            bundle.putLong("timestamp", System.currentTimeMillis()/1000L)
            bundle.putString("call_direction", "in")
            bundle.putString("phone_number", "01012349876")

            dummyCallEventGenerationModule.raiseEvent(EventType.CALL_OFFHOOK, bundle)

        }
        latch.await(10000, TimeUnit.MILLISECONDS)
    }
}