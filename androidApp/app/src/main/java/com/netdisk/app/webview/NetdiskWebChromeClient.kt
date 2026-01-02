package com.netdisk.app.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class NetdiskWebChromeClient(
    private val activity: Activity
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // Clean up previous callback
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        // Launch file picker
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                FILE_CHOOSER_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file chooser", e)
            this.filePathCallback = null
            return false
        }

        return true
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        // You can update a progress bar here if needed
        Log.d(TAG, "Page load progress: $newProgress%")
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = if (resultCode == Activity.RESULT_OK && data != null) {
                data.data?.let { arrayOf(it) }
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    companion object {
        private const val TAG = "NetdiskWebChromeClient"
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }
}
