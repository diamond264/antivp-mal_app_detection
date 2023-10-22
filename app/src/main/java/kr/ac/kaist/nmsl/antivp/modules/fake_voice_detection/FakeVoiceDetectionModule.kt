package kr.ac.kaist.nmsl.antivp.modules.fake_voice_detection

import android.os.Bundle
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import kotlinx.coroutines.*

class FakeVoiceDetectionModule : Module() {
    private val TAG = "FakeVoiceDetection"

    init {
        subscribeEvent(EventType.CALL_OFFHOOK)
    }

    override fun name(): String {
        return "fake_voice_detection"
    }

    override fun handleEvent(type: EventType, bundle: Bundle) {
        when(type) {
            EventType.CALL_OFFHOOK -> {
                Log.d(TAG, "Rcvd a phone call.")

                GlobalScope.launch {
                    if (detectFakeVoice(bundle))
                        raiseEvent(EventType.PHISHING_CALL_DETECTED, bundle)
                }
            }
            else -> {
                Log.e(TAG, "Unexpected event type: $type")
            }
        }
    }

    private fun detectFakeVoice(bundle: Bundle): Boolean {

        bundle.putString("phishing_type", "fake_voice")
        return true
    }
}