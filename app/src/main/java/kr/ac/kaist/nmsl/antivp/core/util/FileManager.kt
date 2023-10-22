package kr.ac.kaist.nmsl.antivp.core.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.util.Log
import java.io.*

class FileManager private constructor(private val mContext: Context) {

    companion object {
        private const val TAG = "FileManager"
        private var instance: FileManager? = null

        // Initialize the FileManager instance with the application context
        fun initialize(context: Context) {
            if (instance == null) {
                instance = FileManager(context.applicationContext)
            }
        }

        // Get the FileManager instance
        fun getInstance(): FileManager {
            if (instance == null) {
                throw IllegalStateException("FileManager has not been initialized")
            }
            return instance!!
        }
    }

    fun getRootDirectory(): String {
//        return Environment.getExternalStorageDirectory().absolutePath
        return mContext.filesDir.absolutePath
    }

    /**
     * Delete the file with the specified filename from the app's internal storage directory.
     *
     * @param filename The name of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     */
    fun deleteFile(filename: String): Boolean {
        val file = File(getRootDirectory(), filename)
        if (file.exists()) {
            return file.delete()
        } else return false
    }

    /**
     * Check if the file with the specified filename exists in the app's internal storage directory.
     *
     * @param filename The name of the file to check for.
     * @return True if the file exists, false otherwise.
     */
    fun fileExists(filename: String): Boolean {
        val file = File(getRootDirectory(), filename)
        return file.exists()
    }

    /**
     * Save the given object to a file with the specified filename in the app's internal storage directory.
     *
     * @param filename The name of the file to save to.
     * @param obj The object to save to the file.
     * @return True if the object was successfully saved to the file, false otherwise.
     */
    fun save(fileName: String, data: Any) {
        val separator = File.separator
        val abs_fileName = File(getRootDirectory(), fileName).absolutePath
        Log.d(TAG, abs_fileName)
        val lastSeparatorIndex = abs_fileName.lastIndexOf(separator)

        // File name contains a path separator, split it into directory and file names
        val directoryName = abs_fileName.substring(0, lastSeparatorIndex)
        val finalFileName = abs_fileName.substring(lastSeparatorIndex + 1)

        // Create the directories if they don't exist
        val directory = File(directoryName)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val fileOutputStream = FileOutputStream(File(directory, finalFileName), true)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)
        Log.d(TAG, ""+File(directory, finalFileName).absolutePath)

        when (data) {
            is String -> {
                fileOutputStream.use { fos ->
                    BufferedWriter(OutputStreamWriter(fos, Charsets.UTF_8)).use { writer ->
                        val writing_data = " "+data+" "
                        writer.append(writing_data)
//                        writer.newLine()
                    }
                }
//                objectOutputStream.writeUTF(data)
            }
            is ByteArray -> objectOutputStream.write(data)
            is Bundle -> {
                val parcel = Parcel.obtain()
                parcel.writeBundle(data)
                val byteArray = parcel.marshall()
                parcel.recycle()
                objectOutputStream.writeObject(byteArray)
            }
            else -> objectOutputStream.writeObject(data)
        }
        objectOutputStream.close()
        fileOutputStream.close()
    }

    /**
     * Load an object from the file with the specified filename in the app's internal storage directory.
     *
     * @param filename The name of the file to load from.
     * @return The loaded object, or null if an error occurred.
     */
    fun load(fileName: String): Any? {
        var data: Any? = null
        try {
            val separator = File.separator
            val abs_fileName = File(getRootDirectory(), fileName).absolutePath
            Log.d(TAG, abs_fileName)
            val lastSeparatorIndex = abs_fileName.lastIndexOf(separator)

            // File name contains a path separator, split it into directory and file names
            val directoryName = abs_fileName.substring(0, lastSeparatorIndex)
            val finalFileName = abs_fileName.substring(lastSeparatorIndex + 1)

            // Create the directories if they don't exist
            val directory = File(directoryName)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileInputStream = FileInputStream(File(directory, finalFileName))
            val objectInputStream = ObjectInputStream(fileInputStream)
            if (fileName.endsWith(".csv") || fileName.endsWith(".tsv") ||
                fileName.endsWith(".txt")) {
                val stringsList = mutableListOf<String>()
                fileInputStream.use { fis ->
                    BufferedReader(InputStreamReader(fis, Charsets.UTF_8)).use { reader ->
                        stringsList.addAll(reader.readLines())
                    }
                }
//                var filteredStringsList = stringsList.map { it.replace(Regex("[^\\x20-\\x7E]"), "") }
                val filteredStringsList = stringsList.map { it.replace(Regex("[^\\p{Print}]"), "") }
                return filteredStringsList.joinToString(' '.toString())
            }

            data = when (val loadedData = objectInputStream.readObject()) {
                is ByteArray -> {
                    if (fileName.endsWith(".wav") || fileName.endsWith(".mp3")) {
                        // if the file is an audio file, create a new MediaPlayer object and load the file
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setDataSource(String(loadedData))
                        mediaPlayer.prepare()
                        mediaPlayer
                    } else {
                        // if the file is a byte array, assume it is a Bundle and deserialize it
                        val parcel = Parcel.obtain()
                        parcel.unmarshall(loadedData, 0, loadedData.size)
                        parcel.setDataPosition(0)
                        Bundle.CREATOR.createFromParcel(parcel)
                    }
                }
                else -> loadedData
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    fun createDirectory(directoryName: String): Boolean {
        val directory = File(getRootDirectory(), directoryName)
        return if (directory.exists()) {
            false
        } else {
            directory.mkdir()
        }
    }

    fun directoryExists(directoryName: String): Boolean {
        val directory = File(getRootDirectory(), directoryName)
        return directory.exists()
    }

    fun removeDirectory(directoryName: String): Boolean {
        val directory = File(getRootDirectory(), directoryName)
        return if (!directory.exists()) {
            false
        } else {
            directory.deleteRecursively()
        }
    }
}
