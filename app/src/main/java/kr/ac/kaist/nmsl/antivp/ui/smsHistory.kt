package kr.ac.kaist.nmsl.antivp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kr.ac.kaist.nmsl.antivp.R
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*

class smsAdapter(context: Context, private val layoutResId: Int, private val data: List<String>, private val onItemClicked: (position: Int) -> Unit)
    : ArrayAdapter<String>(context, layoutResId, data) {    private val seenItems: MutableSet<String> = mutableSetOf()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)

        val textView = view.findViewById<TextView>(R.id.textView)
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val statusButton = view.findViewById<Button>(R.id.statusButton)

        val currentItem = data[position]

        textView.text = currentItem
        imageView.setImageResource(R.drawable.transcribed)

        if (seenItems.contains(currentItem)) {
            statusButton.text = "확인"
            statusButton.setTextColor(Color.BLACK)
        } else {
            statusButton.text = "신규"
            statusButton.setTextColor(Color.GREEN)
        }


        statusButton.setOnClickListener {
            seenItems.add(currentItem)
            notifyDataSetChanged()
            onItemClicked(position)
        }

        return view
    }
}


class smsHistory : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var sortSpinner: Spinner
    private lateinit var adapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()

    val TAG = "SMS History"

    private fun loadSmsFromDirectory(directoryName: String): List<File> {
        val directory = File(filesDir, directoryName)
        return directory.listFiles()?.toList() ?: emptyList()
    }

    private fun sortByDate() {
        items.sortWith(Comparator { o1, o2 ->
            val date1 = o1.substringAfter("(기록일: ").substringBefore(")")
            val date2 = o2.substringAfter("(기록일: ").substringBefore(")")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.parse(date2).compareTo(sdf.parse(date1))
        })
        adapter.notifyDataSetChanged()
    }

    private fun sortByName() {
        items.sortWith(Comparator { o1, o2 ->
            val name1 = o1.substringAfter(". 기록 파일: ").substringBefore(".txt")
            val name2 = o2.substringAfter(". 기록 파일: ").substringBefore(".txt")
            name1.compareTo(name2)
        })
        adapter.notifyDataSetChanged()
    }

    private fun groupByDate() {
        items.sortWith(Comparator { o1, o2 ->
            val date1 = o1.substringAfter("(기록일: ").substringBefore(" HH:mm:ss")
            val date2 = o2.substringAfter("(기록일: ").substringBefore(" HH:mm:ss")
            date2.compareTo(date1)
        })
        adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_history)

        listView = findViewById(R.id.list_view)

        val smsFiles = loadSmsFromDirectory("sms")
        Log.d(TAG, smsFiles.toString())

        smsFiles.forEachIndexed { index, file ->
            var recordDate: Long = 0
            var readableDate: String? = null
            try {
                val attr = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                recordDate = attr.creationTime().toMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                readableDate = sdf.format(recordDate)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file attributes: ${e.message}")
            }
            Log.d(TAG, file.toString())
            items.add("${index+1}. 기록 파일: ${file.name}\n(기록일: $readableDate)")
        }

        adapter = smsAdapter(this, R.layout.list_item_sms, items) { clickedPosition ->
            val intent = Intent(this, DetailedSMSActivity::class.java).apply {
                putExtra("FileName", smsFiles[clickedPosition].name)
            }
            startActivity(intent)
        }
        listView.adapter = adapter
        adapter.notifyDataSetChanged()

        val textView = findViewById<TextView>(R.id.text_view)
        textView.text = "보이스피싱 의심 문자 건수: ${smsFiles.size}"

        sortSpinner = findViewById(R.id.sort_spinner)
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position) {
                    0 -> sortByDate()
                    1 -> sortByName()
                    2 -> groupByDate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}
