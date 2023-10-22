package kr.ac.kaist.nmsl.antivp.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kr.ac.kaist.nmsl.antivp.R
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import com.jaredrummler.apkparser.ApkParser
import kr.ac.kaist.nmsl.antivp.core.util.FileManager
import java.io.FileOutputStream


data class MalAppItem(val appName: String, val detectedDate: String, val iconBitmap: Bitmap)

class MalAppAdapter(context: Context, items: List<MalAppItem>, private val onStatusButtonClicked: (position: Int) -> Unit) : ArrayAdapter<MalAppItem>(context, R.layout.list_item_mal, items) {

    private val seenItems: MutableSet<String> = mutableSetOf()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_mal, parent, false)

        val item = getItem(position)

        val textView: TextView = view.findViewById(R.id.app_info)
        val imageView: ImageView = view.findViewById(R.id.icon_img)
        val statusButton: Button = view.findViewById(R.id.statusButton) // Assuming you have a button in your layout

        textView.text = "${position + 1}. 앱 이름: ${item?.appName}.apk\n(탐지일: ${item?.detectedDate})"
        imageView.setImageBitmap(item?.iconBitmap)

        if (seenItems.contains(item?.appName)) {
            statusButton.text = "확인"
            statusButton.setTextColor(Color.BLACK)
        } else {
            statusButton.text = "신규"
            statusButton.setTextColor(Color.GREEN)
        }

        statusButton.setOnClickListener {
            seenItems.add(item?.appName ?: "")
            notifyDataSetChanged()

            onStatusButtonClicked(position)  // Call the lambda function here
        }
        return view
    }
}

class malHistory : AppCompatActivity() {
    private lateinit var listView: ListView
    val fileManager = FileManager.getInstance()
    val storageDir = fileManager.getRootDirectory()
    private lateinit var adapter: MalAppAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private val items = mutableListOf<String>()

    val TAG = "Malicious App History"

    private fun loadMalDatafromDir(directoryName: String): List<File> {
        val directory = File(filesDir, directoryName)
        return directory.listFiles()?.toList() ?: emptyList()
    }

    private fun sortByDate(adapter: MalAppAdapter) {
        adapter.sort { o1, o2 ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.parse(o2.detectedDate).compareTo(sdf.parse(o1.detectedDate))
        }
    }

    private fun sortByName(adapter: MalAppAdapter) {
        adapter.sort { o1, o2 -> o1.appName.compareTo(o2.appName) }
    }

    private fun groupByDate(adapter: MalAppAdapter) {
        adapter.sort { o1, o2 ->
            val date1 = o1.detectedDate.substringBefore(" HH:mm:ss")
            val date2 = o2.detectedDate.substringBefore(" HH:mm:ss")
            date2.compareTo(date1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mal_history)

        listView = findViewById(R.id.list_view)

        val malFiles = loadMalDatafromDir("malApp")
        Log.d(TAG, malFiles.toString())

        val malAppItems =  malFiles.mapIndexed { index, file ->
            var detectedDate: Long = 0
            var readableDate: String? = null
            try {
                val attr = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                detectedDate = attr.creationTime().toMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                readableDate = sdf.format(detectedDate)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file attributes: ${e.message}")
            }
            Log.d(TAG, file.toString())
            val content = file.readText()
            val cleanedContent = content.substring(content.indexOf("{"))
            val data = JSONObject(cleanedContent)
            val appName = data?.optString("appName") ?: ""
            val iconName = data?.optString("iconName") ?: ""
//            val iconFile = File("Download/$appName/$iconName")

            val iconDir = File("$storageDir/icon")

            val outputFile = File(iconDir, "$appName.png")

            val iconBitmap = BitmapFactory.decodeFile(outputFile.absolutePath)

            MalAppItem(appName, readableDate.toString(), iconBitmap)
        }

        val malAppAdapter = MalAppAdapter(this, malAppItems) { clickedPosition ->
            val intent = Intent(this, DetailedMalActivity::class.java).apply {
                putExtra("FileName", malFiles[clickedPosition].name)
            }
            startActivity(intent)
        }

        listView.adapter = malAppAdapter
//        adapter.notifyDataSetChanged()

        val textView = findViewById<TextView>(R.id.text_view)
        textView.text = "보이스피싱 의심 악성앱 수: ${malFiles.size}"

        sortSpinner = findViewById(R.id.sort_spinner)
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position) {
                    0 -> sortByDate(malAppAdapter)
                    1 -> sortByName(malAppAdapter)
                    2 -> groupByDate(malAppAdapter)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
