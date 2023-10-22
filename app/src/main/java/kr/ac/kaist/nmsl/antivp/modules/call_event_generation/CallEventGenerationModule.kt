package kr.ac.kaist.nmsl.antivp.modules.call_event_generation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kr.ac.kaist.nmsl.antivp.AntiVPApplication
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module

class CallEventGenerationModule : Module(){
    companion object {
        private val TAG = CallEventGenerationModule::class.java.simpleName

        private fun notifyModule(context: Context, eventType: EventType, bundle: Bundle) {
            val moduleManager = (context as AntiVPApplication).getModuleManager()
            val callEvGenModule = moduleManager.getModule("call_event_generation")!!
            callEvGenModule.raiseEvent(eventType, bundle)
        }

        fun notifyRingingEvent(context: Context,
                               timestamp: Long, callDirection: String, phoneNumber: String) {
            val bundle = Bundle()
            bundle.putLong("timestamp", timestamp)  // Unix timestamp in seconds
            bundle.putString("call_direction", callDirection)  // {"in", "out", ""}. Can be empty.
            bundle.putString("phone_number", phoneNumber)  // e.g., "01012349876". Can be empty.

            Log.d(TAG, "notifyRingingEvent: $bundle")
            notifyModule(context, EventType.CALL_RINGING, bundle)
        }

        fun notifyCallStartEvent(context: Context, fileName: String, dialogueId: String) {
            val bundle = Bundle()
            bundle.putString("record_file", fileName)
            bundle.putString("dialogue_id", dialogueId)

            Log.d(TAG, "notifyCallStartEvent: $bundle")
            notifyModule(context, EventType.CALL_OFFHOOK, bundle)
        }

        fun notifyCallEndEvent(context: Context, success: Boolean, uri: Uri?,
                               timestamp: Long, callDirection: String, phoneNumber: String) {
            val bundle = Bundle()
            bundle.putBoolean("success", success)
            bundle.putString("record_file", uri.toString())
            bundle.putLong("timestamp", timestamp)  // Unix timestamp in seconds
            bundle.putString("call_direction", callDirection)  // {"in", "out", ""}. Can be empty.
            bundle.putString("phone_number", phoneNumber)  // e.g., "01012349876". Can be empty.

            Log.d(TAG, "notifyCallEndEvent: $bundle")
            notifyModule(context, EventType.CALL_IDLE, bundle)
        }
    }

    override fun name(): String {
        return "call_event_generation"
    }

    override fun handleEvent(type: EventType, bundle: Bundle) {
    }
}