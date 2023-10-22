package kr.ac.kaist.nmsl.antivp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.kaist.nmsl.antivp.R


class WarningsAdapter : RecyclerView.Adapter<WarningsAdapter.ViewHolder>() {
    private val warnings = ArrayList<String>()
    val TAG = "History Activity"

    fun setWarnings(warningsList: List<String>) {
        warnings.clear()
        warnings.addAll(warningsList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.warning_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parts = warnings[position].split(": ", limit = 2)
        if (parts.size < 2) {
            Log.e(TAG, "Invalid warning format: ${warnings[position]}")
            return
        }
        val timestamp = parts[0]
        val warning = parts[1]

        Log.d(TAG, warning)
        val cleanedString = warning.replace("Bundle[{", "").replace("}]", "")
        val components = cleanedString.split(", ")

        val bundle = Bundle()
        var isPhishingKeyDetected = false
        val phishingComponents = mutableListOf<String>()

        for (component in components) {
            if (isPhishingKeyDetected) {
                if (component.endsWith("]")) {
                    isPhishingKeyDetected = false
                    phishingComponents.add(component.removeSuffix("]"))
                    bundle.putStringArray("phishing", phishingComponents.toTypedArray())
                    phishingComponents.clear() // Clear the list after using it
                } else {
                    phishingComponents.add(component)
                }
                continue
            }
            if (component.startsWith("phishing=[")) {
                val data = component.removePrefix("phishing=[")
                if (data.endsWith("]")) {
                    val phishingData = data.removeSuffix("]").split(", ")
                    bundle.putStringArray("phishing", phishingData.toTypedArray())
                } else {
                    isPhishingKeyDetected = true
                    phishingComponents.add(data)
                }
            } else {
                val (key, value) = component.split("=")
                when (key.trim()) {
                    "cf" -> bundle.putFloat(key.trim(), value.trim().toFloat())
                    "pl" -> bundle.putInt(key.trim(), value.trim().toInt())
                    else -> bundle.putString(key.trim(), value.trim())
                }
            }
        }

        val phishingMessages = bundle.getStringArray("phishing")
        phishingMessages?.let {
            if (it.size > 1) {
                holder.phishingTypeText.text = it[0]
                holder.phishingMessageText.text = it[1]
            } else if (it.isNotEmpty()) {
                holder.phishingTypeText.text = it[0]
                holder.phishingMessageText.text = "Message not provided"
            } else {
                Log.d(TAG, "Message is empty")
            }
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AlarmActivity::class.java)
            intent.putExtra("phishingAlert", bundle)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = warnings.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phishingTypeText: TextView = itemView.findViewById(R.id.phishing_type_text)
        val phishingMessageText: TextView = itemView.findViewById(R.id.phishing_message_text)
    }
}


class HistoryActivity : AppCompatActivity() {
    private lateinit var warningsRecyclerView: RecyclerView
    private lateinit var adapter: WarningsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        warningsRecyclerView = findViewById(R.id.warningsRecyclerView)  // Make sure this ID exists in your XML
        adapter = WarningsAdapter()
        warningsRecyclerView.layoutManager = LinearLayoutManager(this)
        warningsRecyclerView.adapter = adapter

        val sharedPreferences = getSharedPreferences("warnings", Context.MODE_PRIVATE)
        val warnings = sharedPreferences.getStringSet("recentWarnings", mutableSetOf())?.toList() ?: listOf()

        adapter.setWarnings(warnings)
    }
}
