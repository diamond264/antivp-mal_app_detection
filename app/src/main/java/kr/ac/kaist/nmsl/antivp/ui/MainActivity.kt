package kr.ac.kaist.nmsl.antivp.ui

import android.Manifest
import android.content.Context
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.app.ActivityManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kr.ac.kaist.nmsl.antivp.R
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kr.ac.kaist.nmsl.antivp.databinding.ActivityMainBinding
import kr.ac.kaist.nmsl.antivp.modules.mal_app_detection.WatchDogService
//import kr.ac.kaist.nmsl.antivp.service.beAdminAndStartCallTracker

class MainActivity : AppCompatActivity() {
    private val TAG = "kr.ac.kaist.nmsl.antivp.ui.MainActivity"
    private val PERMISSIONS_REQUESTED_FLAG = "permissions_already_requested"
    private var permissionDeniedDialog: AlertDialog? = null
    private val OVERLAY_PERMISSION_REQ_CODE = 1000

    override fun onResume() {
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionDeniedDialog?.dismiss()
    }

    private val PERMISSION_REQUEST_CODE = 1
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.INTERNET,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.VIBRATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Manifest.permission.BIND_INCALL_SERVICE,
        Manifest.permission.BIND_QUICK_SETTINGS_TILE,
        Manifest.permission.BIND_DEVICE_ADMIN,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        Manifest.permission.CAPTURE_AUDIO_OUTPUT,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    private val updateInterval = 3000L
    private val handler = Handler(Looper.getMainLooper())
//    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if (!sharedPreferences.getBoolean(PERMISSIONS_REQUESTED_FLAG, false)) {
            requestPermissions()
            sharedPreferences.edit().putBoolean(PERMISSIONS_REQUESTED_FLAG, true).apply()
        }

        checkForSpecialPermissions()
        setContentView(R.layout.activity_main)

        val statusButton = findViewById<Button>(R.id.status_button)
        statusButton.setOnClickListener {
            val intent = Intent(this, SummaryActivity::class.java)
            startActivity(intent)
        }

        val historyButton = findViewById<Button>(R.id.history_button)
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
        // binding = ActivityMainBinding.inflate(layoutInflater)
        // val view = binding.root
        // setContentView(view)
        //beAdminAndStartCallTracker(this)
        // initActivity(this)

        startUpdates()
    }

    private fun requestPermissions() {
        var shouldRequest = false
        for (permission in permissions) {
            val checkRes = ContextCompat.checkSelfPermission(this, permission)
            if (checkRes != PackageManager.PERMISSION_GRANTED) {
                shouldRequest = true
                break
            }
        }
        if (shouldRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkForSpecialPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions are granted
            val allPermissionsGranted =
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (!allPermissionsGranted) {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("권한 필요")
        builder.setMessage("앱을 사용하기 위한 필수 권한들을 허용해주세요.")
        builder.setPositiveButton("권한 설정하기") { dialog, _ ->
            // Open the app settings screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            // Handle cancel button click
            dialog.dismiss()
        }
        builder.setCancelable(false)
        permissionDeniedDialog = builder.create()
        permissionDeniedDialog?.show()

    }

    private fun startUpdates() {
        val updateRunnable = object: Runnable {
            override fun run () {
//                setTextViews()
                updateServiceStatus()
                handler.postDelayed(this, updateInterval)
            }
        }

        handler.postDelayed(updateRunnable!!, updateInterval)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateServiceStatus() {
        if (isServiceRunning(WatchDogService::class.java)) {
            findViewById<TextView>(R.id.service_status_text).text = "모니터링 실행 중"
        } else {
            findViewById<TextView>(R.id.service_status_text).text = "모니터링이 실행 되고 있지 않습니다."
        }
    }

}

//    fun setTextViews() {
//        val app: AntiVPApplication = application as AntiVPApplication
//        val textBasedDetection = app.getModuleManager().getModule("text_based_detection") as TextBasedDetectionModule
//        val transcribedText = textBasedDetection.getCurrentText()
//        binding.transcribedText.text = transcribedText
//        val detectionResult = textBasedDetection.getCurrentDetectionResult()
//        binding.phishing.text = detectionResult
//    }
