package kr.ac.kaist.nmsl.antivp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kr.ac.kaist.nmsl.antivp.R
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import java.io.File
import java.lang.reflect.Array.set
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


class SummaryActivity : AppCompatActivity() {
    private val TAG = "SummaryActivity"

    private fun detectCounter(directoryName: String): Pair<Int, Int> {
        val directory = File(filesDir, directoryName)
        val allFiles = directory.listFiles() ?: emptyArray()

        var txtFileCount = 0
        var txtFilesTodayCount = 0
        val startOfDayInMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Filter the files that were created today
        for (file in allFiles) {
            if (file.name.endsWith(".txt")) {
                txtFileCount++

                if (file.lastModified() >= startOfDayInMillis) {
                    txtFilesTodayCount++
                }
            }
        }
        return Pair(txtFileCount, txtFilesTodayCount)
    }

    val fileManager = FileManager.getInstance()
    val malDir = "malApp"
    val vpDir = "transcribed"
    val smsDir = "sms"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        val (malAppCount, _) = detectCounter(malDir)

        val (vpCnt, _) = detectCounter(vpDir)
        println(vpCnt)
        val (smsCnt, _) = detectCounter(smsDir)

        findViewById<TextView>(R.id.suspiciousCallsCountTextView).text = "$vpCnt 건"
        val vpDetailsButton = findViewById<Button>(R.id.button_vp)
        vpDetailsButton.setOnClickListener {
            if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                val intent = Intent(this, convHistory::class.java)
                startActivity(intent)
            }
        }

        findViewById<TextView>(R.id.maliciousAppsCountTextView).text = "$malAppCount 건"
        val malDetailsButton = findViewById<Button>(R.id.button_mal_app)
        malDetailsButton.setOnClickListener {
            if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                val intent = Intent(this, malHistory::class.java)
                startActivity(intent)
            }
        }

        findViewById<TextView>(R.id.suspiciousSmsCountTextView).text = "$smsCnt 건"
        val smsDetailsButton = findViewById<Button>(R.id.button_sms)
        smsDetailsButton.setOnClickListener {
            if (!isFinishing && window?.decorView?.windowVisibility == View.VISIBLE) {
                val intent = Intent(this, smsHistory::class.java)
                startActivity(intent)
            }
        }

        if (malAppCount or smsCnt or vpCnt > 0) {//or smsCnt
            findViewById<TextView>(R.id.detectmsg).text = "보이스피싱으로 의심되는 탐지 결과가 있습니다." + "\n" + "자세한 사항은 버튼을 눌러 확인해주세요."
            findViewById<ImageView>(R.id.statusIcon).setImageResource(R.drawable.alert)
        } else {
            findViewById<TextView>(R.id.detectmsg).text = "기기를 모니터링 중입니다."
            findViewById<ImageView>(R.id.statusIcon).setImageResource(R.drawable.safety_icon)
        }
    }
}