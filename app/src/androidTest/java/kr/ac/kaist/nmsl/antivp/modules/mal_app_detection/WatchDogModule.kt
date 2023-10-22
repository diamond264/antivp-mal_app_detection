//package kr.ac.kaist.nmsl.antivp.modules.mal_app_detection
//
//import android.os.Bundle
//import android.os.Environment
//import android.os.FileObserver
//import android.util.Log
//import androidx.test.core.app.ApplicationProvider
//import kotlinx.coroutines.MainScope
//import kotlinx.coroutines.launch
//import kr.ac.kaist.nmsl.antivp.core.EventType
//import kr.ac.kaist.nmsl.antivp.core.Module
//import kr.ac.kaist.nmsl.antivp.core.ModuleManager
//import kr.ac.kaist.nmsl.antivp.modules.phishing_event_receiver.PhishingEventReceiverModule
//import org.junit.Test
//import java.io.File
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//
//internal class WatchDogModuleTest {
//    @Test
//    fun testModule() {
//        val latch = CountDownLatch(2)
//
//        MainScope().launch {
//            val mModuleManager = ModuleManager(ApplicationProvider.getApplicationContext())
//
//            val MalAppDetectionModule = MalAppDetectionModule()
//            val PhishingEventReceiverModule = PhishingEventReceiverModule()
//            val sampleWatchDogModule = object : Module() {
//                private val storageDir = Environment.getExternalStorageDirectory().absolutePath
//                private val downloadDir = File("$storageDir/Download")
//                private val apkExtension = "apk"
//
//                init {
//                    subscribeEvent(EventType.APP_DOWNLOADED)
//                    watchDirectory(downloadDir)
//                }
//
//                override fun name(): String {
//                    return "sample_watchdog_module"
//                }
//
//                private fun watchDirectory(directory: File) {
//                    val observer = object : FileObserver(directory.path, FileObserver.CREATE) {
//                        override fun onEvent(event: Int, path: String?) {
//                            if (path != null && path.endsWith(apkExtension)) {
//                                Log.d(name(), "Detected a new APK file: $path")
//                                // Raise an app_downloaded event with the path of the downloaded APK file
//                                val bundle = Bundle()
//                                bundle.putStringArray("file_path",arrayOf(
//                                    "$directory/$path"),
//                                )
//                                raiseEvent(EventType.APP_DOWNLOADED, bundle)
//                                Log.d(name(), "You are here!!!")
//                            }
//                        }
//                    }
//                    observer.startWatching()
//                }
//                override fun handleEvent(type: EventType, bundle: Bundle) {}
//            }
//            mModuleManager.register(sampleWatchDogModule)
//            mModuleManager.register(MalAppDetectionModule)
//            mModuleManager.register(PhishingEventReceiverModule)
//        }
//        latch.await(1000, TimeUnit.MILLISECONDS)
//    }
//}