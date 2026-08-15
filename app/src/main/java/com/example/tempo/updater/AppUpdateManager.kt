package com.example.tempo.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionTag: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateInfo?>(null)
    val updateState: StateFlow<UpdateInfo?> = _updateState.asStateFlow()

    private val currentVersionName: String
        get() {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "v1.0.0"
            } catch (e: Exception) {
                "v1.0.0"
            }
        }

    suspend fun checkForUpdates() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/teeekaayyy/tempo-habit-tracker/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "TempoApp")

            if (connection.responseCode == 200) {
                val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(rawJson)
                val tagName = jsonObject.optString("tag_name", "")
                val releaseNotes = jsonObject.optString("body", "Bug fixes and performance improvements.")
                val assets = jsonObject.optJSONArray("assets")

                var downloadUrl = ""
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (tagName.isNotEmpty() && downloadUrl.isNotEmpty() && isNewerVersion(tagName, currentVersionName)) {
                    _updateState.value = UpdateInfo(
                        versionTag = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun downloadAndInstall(updateInfo: UpdateInfo) {
        try {
            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "tempo_update_${updateInfo.versionTag}.apk"
            )
            if (destinationFile.exists()) destinationFile.delete()

            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("Downloading Tempo Update ${updateInfo.versionTag}")
                .setDescription("Fetching latest Tempo APK update from GitHub")
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    if (id == downloadId) {
                        context.unregisterReceiver(this)
                        installApk(destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun installApk(file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val remoteClean = remoteTag.removePrefix("v").trim()
        val localClean = localVersion.removePrefix("v").trim()
        return remoteClean != localClean
    }
}
