package kr.ac.kaist.nmsl.antivp.modules.mal_app_detection

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kr.ac.kaist.nmsl.antivp.AntiVPApplication
import java.io.File

class WatchDogService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private lateinit var watchDogModule: WatchDogModule
    val TAG = "WatchDogService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "watchdog_service"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Your Notification Title")
            .setContentText("Your Notification Text")
            .build()
        startForeground(101, notification)
        Log.d(TAG, "WatchDogService Launched")

        val moduleManager = AntiVPApplication.getModuleManagerInstance()
        watchDogModule = moduleManager.getModule("watchdog") as WatchDogModule

        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath)
        watchDogModule.initialize(downloadDir)

        return START_REDELIVER_INTENT
    }
}
