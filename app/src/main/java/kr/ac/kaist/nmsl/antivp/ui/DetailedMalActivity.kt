package kr.ac.kaist.nmsl.antivp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.TextView
import kr.ac.kaist.nmsl.antivp.R
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*


class DetailedMalActivity : AppCompatActivity() {
    val TAG = "Detailed Malicious app activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mal_app_detail)
        val fileName = intent.getStringExtra("FileName")
        val filePath = Paths.get(filesDir.absolutePath, "malApp/$fileName")
        val file = File(filesDir.absolutePath, "malApp/$fileName")
        val content = file.readText()
        val cleanedContent = content.substring(content.indexOf("{"))
        val data = JSONObject(cleanedContent)
//        val currentMalAppCount = data?.getInt("malAppCount") ?: 0
        val appName = data?.optString("appName") ?: ""
        val secretClassesDexMsg = data?.optString("secretClassesDexMsg") ?: ""
        val dnsslMsg = data?.optString("dnsslMsg") ?: ""
        val nestedApkMsg = data?.optString("nestedApkMsg") ?: ""
        val encryptedResourceMsg = data?.optString("encryptedResourceMsg") ?: ""
        val kkdataMsg = data?.optString("kkdataMsg") ?: ""

        val titleTextView = findViewById<TextView>(R.id.title_textview)
        titleTextView.text = "보이스피싱 앱이 탐지되었습니다."

        var detectedDate: Long = 0
        var readableDate: String? = null
        try {
            val attr = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            detectedDate = attr.creationTime().toMillis()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            readableDate = sdf.format(detectedDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file attributes: ${e.message}")
        }
        val descriptionTextView = findViewById<TextView>(R.id.description_textview)
        descriptionTextView.text = Html.fromHtml(
                    "<b>앱 이름:</b> ${if (appName != null && appName != "") "$appName.apk" else "해당없음"}<br/><br/>" +
                    "<b>암호화된 악성 .dex 파일:</b> ${if (secretClassesDexMsg != null && secretClassesDexMsg != "") secretClassesDexMsg else "해당없음"}<br/><br/>" +
                    "<b>동적 라이브러리 로드:</b> ${if (dnsslMsg != null && dnsslMsg != "") dnsslMsg else "해당없음"}<br/><br/>" +
                    "<b>내장된 APK 파일:</b> ${if (nestedApkMsg != null && nestedApkMsg != "") nestedApkMsg else "해당없음"}<br/><br/>" +
                    "<b>암호화 된 리소스 파일:</b> ${if (encryptedResourceMsg != null && encryptedResourceMsg != "") encryptedResourceMsg else "해당없음"}<br/><br/>" +
                    "<b>바이너리 데이터 저장:</b> ${if (kkdataMsg != null && kkdataMsg != "") kkdataMsg else "해당없음"}<br/><br/>"
        )
        Log.d(TAG, "File creation date: $readableDate")
        val dateTextView = findViewById<TextView>(R.id.date_textview)
        dateTextView.text = "탐지 일: $readableDate"
    }
}
