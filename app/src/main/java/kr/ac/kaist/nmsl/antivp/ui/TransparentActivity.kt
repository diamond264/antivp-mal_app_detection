package kr.ac.kaist.nmsl.antivp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kr.ac.kaist.nmsl.antivp.core.EventType


class TransparentActivity : AppCompatActivity() {
    private val TAG = "TransparentActivity"

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            showDialog(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)  // update the intent to the new one
        showDialog(intent)
    }

    override fun onDestroy() {
        alertDialog?.dismiss()  // Dismiss the dialog if it's showing
        alertDialog = null
        super.onDestroy()
    }

    private fun logBundleContents(tag: String, bundle: Bundle?) {
        if (bundle == null) {
            Log.d(tag, "Bundle is null")
            return
        }

        for (key in bundle.keySet()) {
            Log.d(tag, "$key = ${bundle.get(key)}")
        }
    }

    private fun showDialog(intent: Intent?) {
        val eventData = intent?.getBundleExtra("phishingAlert")
        logBundleContents(TAG, eventData)
        val eventTypeString = intent?.getStringExtra("eventType")
        val eventType = eventTypeString?.let { EventType.valueOf(it) }

        val title = when (eventType) {
            EventType.PHISHING_CALL_DETECTED -> "보이스피싱 전화 경고"
            EventType.PHISHING_APP_DETECTED -> "보이스피싱 앱 경고"
            EventType.SMISHING_SMS_DETECTED -> "스미싱 문자 경고"
            else -> "Unknown Alert"
        }

        val message = when (eventType) {
            EventType.PHISHING_CALL_DETECTED -> "보이스피싱 통화가 탐지되었습니다."
            EventType.PHISHING_APP_DETECTED -> "보이스피싱 앱 다운로드가 탐지되었습니다."
            EventType.SMISHING_SMS_DETECTED -> "스미싱 문자메시지가 탐지되었습니다."
            else -> "Unknown event detected."
        }

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton("AntiVP을 열어 자세히 보기") { _, _ ->
                val AlarmActivityIntent = Intent(this, AlarmActivity::class.java)
                eventData?.let {
                    AlarmActivityIntent.putExtra("phishingAlert", it)
                }
                startActivity(AlarmActivityIntent)
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .create()

        alertDialog.show()
    }
}