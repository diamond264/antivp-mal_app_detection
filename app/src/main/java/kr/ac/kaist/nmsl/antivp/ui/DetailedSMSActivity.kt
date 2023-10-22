package kr.ac.kaist.nmsl.antivp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.TextView
import kr.ac.kaist.nmsl.antivp.R
import java.text.SimpleDateFormat
import java.util.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.io.File


class DetailedSMSActivity : AppCompatActivity() {
    val TAG = "Detailed SMS activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_detail)
//        val fileManager = FileManager.getInstance()
        val fileName = intent.getStringExtra("FileName")
        if (fileName != null) {
            Log.d(TAG, fileName)
        }
        val filePath = Paths.get(filesDir.absolutePath, "sms/$fileName")

        val smsText = try {
            val content = File(filePath.toString()).readText(Charsets.UTF_8)
            cleanString(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file content: ${e.message}")
            ""
        }
        var detectedDateMillis: Long = 0
        try {
            val attr = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            detectedDateMillis = attr.creationTime().toMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file attributes: ${e.message}")
        }

        val titleTextView = findViewById<TextView>(R.id.title_textview)
        titleTextView.text = "보이스피싱 문자 내용"

        val detectedDate = if (detectedDateMillis > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(detectedDateMillis))
        } else {
            "Unknown"
        }
        val descriptionTextView = findViewById<TextView>(R.id.description_textview)
        descriptionTextView.text = Html.fromHtml(
            "<b>문자 내용:</b> $smsText<br/><br/>"
        )

        val dateTextView = findViewById<TextView>(R.id.date_textview)
        dateTextView.text = "기록일: $detectedDate"
    }
    private fun cleanString(input: String): String {
        val unwantedCharacters = Regex("[\uFEFF-\uFFFF]") // Matches BOM and other uncommon characters.
        return input.replace(unwantedCharacters, "")
    }
}
