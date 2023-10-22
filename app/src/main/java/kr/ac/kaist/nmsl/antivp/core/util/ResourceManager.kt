package kr.ac.kaist.nmsl.antivp.core.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kr.ac.kaist.nmsl.antivp.modules.model_optimization.ModelType
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ResourceManager(applicationContext: Context) {

    private val applicationContext = applicationContext
    private var timer: Timer? = null
    private var outputStream: FileOutputStream? = null
    private var writer: PrintWriter? = null
    private val TAG = "ResourceManager"
    private var totalMem = 0.0
    private var totalCpu = 0.0
    private var modelStatus = "NONE"

    fun startMonitoring() {
        if (timer == null) {
            timer = Timer()
        }
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    val dateString = getDateString()

                    if (writer == null || outputStream == null) {
                        val file = File(applicationContext.filesDir, "resource_usage.csv")
                        outputStream = FileOutputStream(file, true)
                        writer = PrintWriter(outputStream)
                    }

                    // memory usage
                    val memoryUsage = readMemInfo()
                    // read CPU usage (require root permission)
                    val cpuUsage = readStat()

                    writer!!.println("$dateString, $modelStatus, $memoryUsage, $cpuUsage")
                    writer!!.flush()

                } catch (e: IOException) {
                    Log.e(TAG, "Error while writing resource_usage.csv")
                }
            }
        }, 0, 1000)

    }

    fun stopMonitoring() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }

        if (writer != null) {
            writer!!.close()
            writer = null
        }

        if (outputStream != null) {
            outputStream!!.close()
            outputStream = null
        }

    }

    fun updateModelStatus(isInferencing: Boolean, modelPath: String) {
        modelStatus = if (!isInferencing) "NONE" else modelPath
    }

    fun getDateString(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(currentTimeMillis))
    }

    private fun readMemInfo(): Double {
        lateinit var reader: RandomAccessFile
        val getTotalMem = (totalMem == 0.0)
        var available = -1.0
        var inUse = -1.0

        try {
            reader = RandomAccessFile("/proc/meminfo", "r")
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        var line = reader.readLine()
        while (line != null) {
            if (getTotalMem && line.startsWith("MemTotal:")) {
                totalMem = line.split("\\s+".toRegex())[1].toDouble()
            } else if (line.startsWith("MemAvailable:")) {
                available = line.split("\\s+".toRegex())[1].toDouble()
            } else if (line.startsWith("MemFree:")) {
                available = line.split("\\s+".toRegex())[1].toDouble()
            }
            if (totalMem != 0.0 && available != -1.0) {
                inUse = totalMem - available
                break
            }
            line = reader.readLine()

        }
        reader.close()

        return inUse
    }

    private fun readStat(): Double {
        val getTotalCpu = (totalCpu == 0.0)
        try {
            val cmdline = arrayOf("su", "sh", "-c", "cat /proc/stat")
            val readStat = Runtime.getRuntime().exec(cmdline)
            val reader = BufferedReader(InputStreamReader(readStat.inputStream))
            val buffer = CharArray(4096)
            val output = StringBuffer()
            readStat.waitFor()

            var read = reader.read(buffer)
            output.append(buffer, 0, read)

            val cpuStats = output.toString().split("\\s+".toRegex())
            if (getTotalCpu) {
                for (i in 1..7) {
                    totalCpu += cpuStats[i].toDouble()
                }
            }
            val idle = cpuStats[4].toDouble()

            reader.close()
            return totalCpu - idle
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.0
    }

}