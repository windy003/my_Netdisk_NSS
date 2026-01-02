package com.netdisk.app.storage

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager

class NetdiskDownloadManager(private val context: Context) {

    fun enqueueDownload(url: String, filename: String): Long {
        // Get authentication cookies
        val cookies = getCookieHeader(url)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Downloading from Netdisk")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            // Save to Downloads/Netdisk directory
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Netdisk/$filename"
            )

            // Add authentication cookies
            if (cookies.isNotEmpty()) {
                addRequestHeader("Cookie", cookies)
            }

            // Allow both WiFi and mobile data
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                DownloadManager.Request.NETWORK_MOBILE
            )

            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    private fun getCookieHeader(url: String): String {
        return CookieManager.getInstance().getCookie(url) ?: ""
    }
}
