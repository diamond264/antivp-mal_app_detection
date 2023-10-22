package kr.ac.kaist.nmsl.antivp.modules.phishing_event_receiver

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.util.Log
import android.view.WindowManager
import kr.ac.kaist.nmsl.antivp.AntiVPApplication
import kr.ac.kaist.nmsl.antivp.R
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import kr.ac.kaist.nmsl.antivp.ui.AlarmActivity
import kr.ac.kaist.nmsl.antivp.ui.TransparentActivity


class PhishingEventReceiverModule : Module() {
    private val TAG = "PhishingEventReceiverModule"

    init {
        subscribeEvent(EventType.PHISHING_CALL_DETECTED)
        subscribeEvent(EventType.PHISHING_APP_DETECTED)
        subscribeEvent(EventType.SMISHING_SMS_DETECTED)
    }

    override fun name(): String {
        return "phishing_event_receiver"
    }

    @SuppressLint("LongLogTag")
    override fun handleEvent(type: EventType, bundle: Bundle) {
        val context = AntiVPApplication.getContext()

        when (type) {
            EventType.PHISHING_CALL_DETECTED -> {
                Log.d(TAG, "Received a suspicious phone call.")
                alarming(context)
                showNotification(context, type, bundle)
                showAlertWithOpenAppButton(context, type, bundle)
            }
            EventType.SMISHING_SMS_DETECTED  -> {
                Log.d(TAG, "Received a suspicious text message.")
                alarming(context)
                showNotification(context, type, bundle)
                showAlertWithOpenAppButton(context, type, bundle)
            }
            EventType.PHISHING_APP_DETECTED -> {
                Log.d(TAG, bundle.toString())
                alarming(context)
                showNotification(context, type, bundle)
                showAlertWithOpenAppButton(context, type, bundle)
            }
            else -> {
                Log.e(TAG, "Unexpected event type: $type")
            }
        }
    }

    @SuppressLint("LongLogTag")
    private fun showNotification(context: Context, type: EventType, bundle: Bundle) {
        val AlarmActivityIntent = Intent(context, AlarmActivity::class.java)
        AlarmActivityIntent.putExtra("phishingAlert", bundle)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, AlarmActivityIntent, pendingIntentFlags)

        val notificationBuilder = Notification.Builder(context)
            .setContentTitle(getNotificationTitle(type))
            .setContentText(getNotificationText(type))
            .setSmallIcon(R.drawable.ic_phishing_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "phishing_alerts"
            val channel = NotificationChannel(channelId, "Phishing Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            notificationBuilder.setChannelId(channelId)
        }
        val notification = notificationBuilder.build()
        val notificationId = when(type) {
            EventType.PHISHING_CALL_DETECTED -> 1
            EventType.PHISHING_APP_DETECTED -> 2
            EventType.SMISHING_SMS_DETECTED -> 3
            else -> 0
        }
        notificationManager.notify(notificationId, notification)
    }

    private fun getNotificationTitle(type: EventType): String {
        return when (type) {
            EventType.PHISHING_CALL_DETECTED -> "보이스피싱 경고"
            EventType.PHISHING_APP_DETECTED -> "보이스피싱 앱 경고"
            EventType.SMISHING_SMS_DETECTED -> "스미싱 문자 경고"
            else -> "Notification"
        }
    }

    private fun getNotificationText(type: EventType): String {
        return when (type) {
            EventType.PHISHING_CALL_DETECTED -> "보이스피싱 전화통화가 감지되었습니다."
            EventType.PHISHING_APP_DETECTED -> "악성 앱 다운로드가 감지되었습니다."
            EventType.SMISHING_SMS_DETECTED -> "스미싱 문자가 감지되었습니다."
            else -> "Check your device for details."
        }
    }

    private fun alarming(context: Context) {
        for (i in 0..4) { // Play the beep for 5 times
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 5000)
            Thread.sleep(500) // Wait for 5 seconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationPattern = longArrayOf(0, 5000) // Vibrate for 5000 ms
                val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1) // -1 means "don't repeat"
                vibrator.vibrate(vibrationEffect)
            } else {
                val vibrationPattern = longArrayOf(0, 5000) // Vibrate for 5000 ms
                vibrator.vibrate(vibrationPattern, -1) // -1 means "don't repeat"
            }
        }
    }
    private fun showAlertWithOpenAppButton(context: Context, type: EventType, bundle: Bundle) {
        val notificationId = when(type) {
            EventType.PHISHING_CALL_DETECTED -> 1
            EventType.PHISHING_APP_DETECTED -> 2
            EventType.SMISHING_SMS_DETECTED -> 3
            else -> 0
        }

        val intent = Intent(context, TransparentActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("phishingAlert", bundle)
        intent.putExtra("eventType", type.name)
        context.startActivity(intent)
    }
}