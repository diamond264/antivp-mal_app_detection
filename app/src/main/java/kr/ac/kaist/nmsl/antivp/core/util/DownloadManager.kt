package kr.ac.kaist.nmsl.antivp.core.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class DownloadManager(context: Context) {
    private val TAG = "DownloadManager"
    val fileManager = FileManager.getInstance()
    val mContext = context

    fun downloadModelFiles() {
        val assetManager = mContext.assets
        val filesDir = mContext.filesDir.absolutePath + "/models"
        Log.d("DownloadManager", filesDir)

        // List all files in the "assets" directory
        val assetFiles = assetManager.list("") ?: return

        // Copy each file from assets to filesDir
        for (fileName in assetFiles) {
            val assetFilePath = "$fileName"
            val targetFile = File(filesDir, fileName)

            if (targetFile.parentFile?.exists() == false) {
                targetFile.parentFile?.mkdirs()
            }

            // Copy the file only if it doesn't exist in the filesDir
            if (!targetFile.exists()) {
                try {
                    Log.d(TAG, assetFilePath)
                    val inputStream: InputStream = assetManager.open(assetFilePath)
                    val outputStream = FileOutputStream(targetFile)

                    inputStream.copyTo(outputStream)

                    inputStream.close()
                    outputStream.close()
                } catch (e: IOException) {
                    // Handle any exceptions that may occur during copying
                    e.printStackTrace()
                }
            }
        }
//        GlobalScope.launch {
//            downloadFile("models/stt.ptl", "speech_to_text.ptl")
//            downloadFile("models/bert.pt", "text_detection.pt")
//            downloadFile("models/voice_sample.wav", "voice_sample_1.wav")
//            downloadFile("models/vocab.txt", "vocab.txt")
//        }
    }

    fun deleteModelFiles() {
        removeFile("models/speech_to_text.ptl")
        removeFile("models/text_detection.pt")
        removeFile("models/voice_sample.wav")
        removeFile("models/vocab.txt")
        removeFile("models")
    }

    fun removeFile(localFileName: String): Int {
        val basePath = fileManager.getRootDirectory()
        val localFilePath = "$basePath/$localFileName"
        val file = File(localFilePath)
        if (file.exists()) {
            file.delete()
            return 1
        }
        if (file.isDirectory) {
            file.delete()
            return 1
        }
        return 0
    }

    fun downloadFile(localFileName: String, remoteFileName: String) {
        val basePath = fileManager.getRootDirectory()
        val localFilePath = "$basePath/$localFileName"
        val localFile = File(localFilePath)

        if (localFile.parentFile?.exists() == false) {
            localFile.parentFile?.mkdirs()
        }

        if (localFile.exists()) {
            Log.e(TAG, "file $localFileName exists")
            return
        }

        val serverUrl = "http://suzy.kaist.ac.kr:11197/download/$remoteFileName"

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val client = OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()
            val request = Request.Builder()
                .url(serverUrl)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body

            if (response.isSuccessful && body != null) {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(localFile)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()

                Log.d(TAG, "File downloaded successfully: ${localFile.absolutePath}")
            } else {
                Log.e(TAG, "Failed to download file")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Exception occurred during file download", e)
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}