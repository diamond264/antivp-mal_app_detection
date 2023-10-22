package kr.ac.kaist.nmsl.antivp.modules.mal_app_detection

import android.os.Bundle
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import kr.ac.kaist.nmsl.antivp.core.EventType
import kr.ac.kaist.nmsl.antivp.core.Module
import java.io.File

class WatchDogModule : Module() {
    private lateinit var observer: FileObserver
    private lateinit var downloadDir: File
    private val apkExtension = "apk"
    private val TAG = "WatchdogModule"

    override fun name(): String {
        return "watchdog"
    }

    fun initialize(downloadDir: File) {
        this.downloadDir = downloadDir
        startWatching()
    }

    private fun startWatching() {
        observer = object : FileObserver(downloadDir.path,
            FileObserver.CLOSE_WRITE or FileObserver.CREATE or FileObserver.MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                when (event) {
                    FileObserver.CREATE -> {
                        Log.d(TAG, "File created: $path")
                        if (path != null && path.endsWith(apkExtension, ignoreCase = true)) {
                            Log.d(TAG, "Potential APK file: $path")
                        } else {
                            Log.d(TAG, "Ignoring non-APK file event: CREATE, path: $path")
                        }
                    }
                    FileObserver.MOVED_TO -> {
                        // Handle the case when a file is renamed to its proper name
                        Log.d(TAG, "(final)File moved/renamed to: $path")
                        if (path != null && path.endsWith(apkExtension, ignoreCase = true)) {
                            handlePotentialApkFile(path)
                        }
                    }
                }
            }
        }
        observer.startWatching()
        Log.d(TAG, "Started watching directory: ${downloadDir.path}")
    }

    private fun handlePotentialApkFile(path: String) {
        val apkFile = File(downloadDir, path)
        if (apkFile.exists()) {
            Log.d(TAG, "Detected a new APK file: $path")
            // Raise an app_downloaded event with the path of the downloaded APK file
            val bundle = Bundle().apply {
                putStringArray("file_path", arrayOf(apkFile.absolutePath))
            }
            raiseEvent(EventType.APP_DOWNLOADED, bundle)
            Log.d(TAG, "Download Event Triggered")
        } else {
            Log.e(TAG, "APK file does not exist: $apkFile")
        }
    }


    override fun handleEvent(type: EventType, bundle: Bundle) {
    }
}
