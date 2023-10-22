package kr.ac.kaist.nmsl.antivp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.anastr.speedviewlib.Speedometer
import kr.ac.kaist.nmsl.antivp.R


class AlarmActivity : AppCompatActivity() {
    private lateinit var phishingWarningText: TextView
    private lateinit var speedometer: Speedometer
    private val TAG = "AlarmActivity"

    companion object {
        const val REQUEST_SYSTEM_ALERT_WINDOW_PERMISSION = 100
    }
    private fun saveWarningToPrefs(bundle: Bundle) {
        val sharedPreferences = getSharedPreferences("warnings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentWarnings = sharedPreferences.getStringSet("recentWarnings", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val currentTimestamp = System.currentTimeMillis().toString()
        val uniqueWarning = "$currentTimestamp: ${bundle.toString()}"

        currentWarnings.add(uniqueWarning)

        editor.putStringSet("recentWarnings", currentWarnings).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phishingBundle = intent.getBundleExtra("phishingAlert")
        Log.d(TAG, phishingBundle.toString())
        if (phishingBundle == null) {
            setContentView(R.layout.activity_summary)
            Log.d(TAG, "onCreate called")
            Log.d(TAG, "OMG!!!")
        } else {
            setContentView(R.layout.activity_alarm2)
            speedometer = findViewById(R.id.speedView)
            phishingWarningText = findViewById(R.id.phishing_warning_text)
            speedometer.unit = ""
            speedometer.minSpeed = 0F
            speedometer.maxSpeed = 100F
            speedometer.withTremble = false
            warningUI(phishingBundle)
        }
    }
    override fun onResume() {
        super.onResume()
        val phishingBundle = intent.getBundleExtra("phishingAlert")
        if (phishingBundle == null) {
            val summaryIntent = Intent(this, SummaryActivity::class.java)
            startActivity(summaryIntent)
            finish()
        } else {
            warningUI(phishingBundle)
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, SummaryActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val phishingBundle = intent.getBundleExtra("phishingAlert")
        if (phishingBundle != null) {
            warningUI(phishingBundle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speedometer.isInitialized) {
            speedometer.stop()
        }
    }

    private fun warningUI(phishingBundle: Bundle?) {
        val viewDetailsButton = findViewById<Button>(R.id.view_details_button)
        val msg = phishingBundle?.getStringArray("phishing")
        if (phishingBundle != null) {
            val phishingType = msg?.get(0)
            Log.d(TAG, phishingType.toString())
            val phishingMsg = msg?.get(1)
            val phishingCF = phishingBundle.getFloat("cf")

            val phishingWarningText = findViewById<TextView>(R.id.phishing_warning_text)
            phishingWarningText.text = "경고: $phishingMsg"
            val phishingTypeText = findViewById<TextView>(R.id.type_message_text)
            if(phishingType == "보이스피싱앱") {
                val appName  = phishingBundle.getString("appName")
                phishingTypeText.text = "보이스피싱 앱 다운로드가 탐지되었습니다."
                    .plus(" 현재 다운로드 된 앱($appName.apk)은 $phishingType 으로 의심됩니다.")
                    .plus(" 신뢰할 수 있는 출처로부터 앱을 다운받으셨는지 확인해주세요.\n\n")
                    .plus(" 해당 앱은 AntiVP 앱에 의해 삭제되었습니다.")
                    .plus(" 파일이 보이스피싱 앱으로 탐지된 이유를 보시려면 아래 버튼을 클릭해주세요.")

                viewDetailsButton.setOnClickListener {
                    if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                        val intent = Intent(this, malHistory::class.java)
                        startActivity(intent)
                    }
                }
            } else if (phishingType == "보이스피싱") {
                phishingTypeText.text = "현재 통화중인 내용은 보이스피싱으로 의심됩니다."
                    .plus(" 즉시 전화를 끊고 경찰에 신고하시기 바랍니다.")
                    .plus(" 통화내용은 저희 AntiVP 앱에 의해 모두 기록되었습니다.")
                    .plus(" 통화내용이 보이스피싱으로 탐지된 자세한 이유를 알고 싶으시다면 아래의 버튼을 눌러주세요.")
                viewDetailsButton.setOnClickListener {
                    if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                        val intent = Intent(this, convHistory::class.java)
                        startActivity(intent)
                    }
                }
            } else if (phishingType == "스미싱") {
                phishingTypeText.text = "스미싱으로 의심되는 문자메시지를 받으셨습니다."
                    .plus(" 방금 받으신 문자메시지는 스미싱 문자로 의심됩니다..")
                    .plus(" 개인정보가 유출 될 수 있으니 절대 답장이나 통화버튼, 또는 링크를 누르지 마세요.")
                    .plus(" 문자메시지가 $phishingType 으로 의심되는 이유를 알고 싶으시다면 버큰을 눌러 확인해주세요.")
                viewDetailsButton.setOnClickListener {
                    if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                        val intent = Intent(this, smsHistory::class.java)
                        startActivity(intent)
                    }
                }
            }

//            val confidenceScoreText = findViewById<TextView>(R.id.score_text)
//            val confidenceScore = String.format("%.2f", phishingCF * 100)
//            confidenceScoreText.text = "신뢰도 점수: $confidenceScore%"

            phishingWarningText.visibility = View.VISIBLE
            phishingTypeText.visibility = View.VISIBLE
//            confidenceScoreText.visibility = View.INVISIBLE

            if (::speedometer.isInitialized) {
                speedometer.speedPercentTo((phishingCF*100).toInt())
            } else {
                Log.e(TAG, "Speedometer is not initialized")
            }
        }
        phishingBundle?.let { saveWarningToPrefs(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SYSTEM_ALERT_WINDOW_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permission granted. System alert window can be displayed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. System alert window will not be displayed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}