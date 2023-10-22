package kr.ac.kaist.nmsl.antivp.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.EventLog.Event
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kr.ac.kaist.nmsl.antivp.AntiVPApplication
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.ModuleManager
//import kr.ac.kaist.nmsl.antivp.modules.call_event_generation.CallBroadcastReceiver
import kr.ac.kaist.nmsl.antivp.ui.MainActivity

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kr.ac.kaist.nmsl.antivp.R
import kr.ac.kaist.nmsl.antivp.bcr.Permissions
import kr.ac.kaist.nmsl.antivp.bcr.Preferences

fun initActivity(activity: AppCompatActivity) {
    try {
        beAdminAndGrantPermission(activity)

        // Set call recording
        val prefs = Preferences(activity.applicationContext)
        prefs.isCallRecordingEnabled = true

        // Register applicationContext to modules
        val moduleManager = (activity.application as AntiVPApplication).getModuleManager()
        moduleManager.setApplicationContext(activity.applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun beAdminAndGrantPermission(activity: AppCompatActivity) { // was beAdminAndStartCallTracker by HJ
    val appctx = activity.applicationContext

    // Require Device Admin
    val mDPM = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val mAdminName = ComponentName(appctx, AntiVPDeviceAdminReceiver::class.java)
    if (!mDPM.isAdminActive(mAdminName)) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Click on Activate button to secure your application.")
        activity.startActivity(intent)
    }

    // Grant permissions
    if (!Permissions.haveRequired(appctx)) {
        val requestPermissionRequired =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
                // Call recording can still be enabled if optional permissions were not granted
                if (!granted.all { it.key !in Permissions.REQUIRED || it.value }) {
                    activity.startActivity(Permissions.getAppInfoIntent(appctx))
                }
            }

        // Ask for optional permissions the first time only
        requestPermissionRequired.launch(Permissions.REQUIRED + Permissions.OPTIONAL)
    }

    // Inhibit battery optimization
    if (!Permissions.isInhibitingBatteryOpt(appctx)) {
        Permissions.getInhibitBatteryOptIntent(appctx)
    }
}

fun initCallRecorderNotification(application: Application) {
    createPersistentChannel(application)
    createAlertsChannel(application)
}

/**
 * Create a low priority notification channel for the persistent notification.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun createPersistentChannel(application: Application) {
    val name = application.getString(R.string.notification_channel_persistent_name)
    val description = application.getString(R.string.notification_channel_persistent_desc)
    val channel = NotificationChannel(
        AntiVPApplication.CHANNEL_ID_PERSISTENT, name, NotificationManager.IMPORTANCE_LOW)
    channel.description = description

    val notificationManager = application.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)
}

/**
 * Create a high priority notification channel for alerts.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun createAlertsChannel(application: Application) {
    val name = application.getString(R.string.notification_channel_alerts_name)
    val description = application.getString(R.string.notification_channel_alerts_desc)
    val channel = NotificationChannel(
        AntiVPApplication.CHANNEL_ID_ALERTS, name, NotificationManager.IMPORTANCE_HIGH)
    channel.description = description

    val notificationManager = application.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)
}