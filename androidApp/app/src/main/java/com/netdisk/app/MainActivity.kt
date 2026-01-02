package com.netdisk.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.netdisk.app.services.AudioPlaybackService
import com.netdisk.app.storage.NetdiskDownloadManager
import com.netdisk.app.storage.PreferencesManager
import com.netdisk.app.ui.SettingsActivity
import com.netdisk.app.webview.JavaScriptBridge
import com.netdisk.app.webview.NetdiskWebChromeClient
import com.netdisk.app.webview.NetdiskWebViewClient

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var downloadManager: NetdiskDownloadManager
    private lateinit var webChromeClient: NetdiskWebChromeClient

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioPlaybackService.ACTION_PLAYBACK_STATE_CHANGED -> {
                    val state = intent.getStringExtra(AudioPlaybackService.EXTRA_STATE) ?: return
                    updateWebViewPlaybackState(state)
                }
                AudioPlaybackService.ACTION_PLAYBACK_ERROR -> {
                    notifyWebViewOfError()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize managers
        preferencesManager = PreferencesManager(this)
        downloadManager = NetdiskDownloadManager(this)

        // Check if this is first launch (no server configured)
        if (isFirstLaunch()) {
            // Redirect to settings to configure server
            startActivityForResult(Intent(this, SettingsActivity::class.java), REQUEST_INITIAL_SETUP)
            return
        }

        // Setup WebView
        webView = findViewById(R.id.webView)
        setupWebView()

        // Register broadcast receiver for playback state updates
        registerPlaybackReceiver()

        // Load server URL
        loadServerUrl()
    }

    private fun isFirstLaunch(): Boolean {
        // Check if server host is still the default value
        val serverHost = preferencesManager.getServerHost()
        val isDefault = serverHost == "192.168.1.100"

        // Also check if user has never saved any config
        val hasConfigured = preferencesManager.hasConfigured()

        return isDefault && !hasConfigured
    }

    private fun setupWebView() {
        android.util.Log.d("MainActivity", "setupWebView() called")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true  // For localStorage
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)

            // Enable caching for better performance
            cacheMode = WebSettings.LOAD_DEFAULT

            // Mixed content for HTTP servers
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // Enable zoom controls (optional)
            builtInZoomControls = true
            displayZoomControls = false

            // User agent (optional - can help with compatibility)
            userAgentString = "$userAgentString NetdiskAndroid/1.0"
        }

        android.util.Log.d("MainActivity", "WebView settings configured")

        // Set WebView clients
        webView.webViewClient = NetdiskWebViewClient(preferencesManager)
        webChromeClient = NetdiskWebChromeClient(this)
        webView.webChromeClient = webChromeClient

        android.util.Log.d("MainActivity", "WebView clients set")

        // Inject JavaScript bridge
        val jsBridge = JavaScriptBridge(this, downloadManager, preferencesManager)
        webView.addJavascriptInterface(jsBridge, "Android")

        android.util.Log.d("MainActivity", "JavaScript bridge injected")

        // Restore cookies if available
        restoreCookies()

        android.util.Log.d("MainActivity", "setupWebView() completed")
    }

    private fun loadServerUrl() {
        val serverUrl = preferencesManager.getServerUrl()
        android.util.Log.d("MainActivity", "Loading server URL: $serverUrl")

        if (!::webView.isInitialized) {
            android.util.Log.e("MainActivity", "ERROR: WebView not initialized when trying to load URL!")
            android.widget.Toast.makeText(this, "错误: WebView未初始化", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        android.widget.Toast.makeText(this, "正在加载: $serverUrl", android.widget.Toast.LENGTH_SHORT).show()
        webView.loadUrl(serverUrl)
        android.util.Log.d("MainActivity", "WebView.loadUrl() called successfully")
    }

    private fun restoreCookies() {
        val savedCookies = preferencesManager.getAuthCookies()
        if (savedCookies != null) {
            val serverUrl = preferencesManager.getServerUrl()
            CookieManager.getInstance().setCookie(serverUrl, savedCookies)
        }
    }

    private fun registerPlaybackReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioPlaybackService.ACTION_PLAYBACK_STATE_CHANGED)
            addAction(AudioPlaybackService.ACTION_PLAYBACK_ERROR)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(playbackStateReceiver, filter)
    }

    private fun updateWebViewPlaybackState(stateJson: String) {
        // Call JavaScript function to update UI
        webView.post {
            webView.evaluateJavascript(
                "if (typeof window.onNativePlaybackStateChanged === 'function') { window.onNativePlaybackStateChanged('$stateJson'); }",
                null
            )
        }
    }

    private fun notifyWebViewOfError() {
        webView.post {
            webView.evaluateJavascript(
                "if (typeof window.onNativePlaybackError === 'function') { window.onNativePlaybackError(); }",
                null
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        android.util.Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        when (requestCode) {
            REQUEST_INITIAL_SETUP -> {
                val isFirst = isFirstLaunch()
                android.util.Log.d("MainActivity", "isFirstLaunch=$isFirst, resultCode=$resultCode, RESULT_OK=$RESULT_OK")

                if (resultCode == RESULT_OK || !isFirst) {
                    // Setup completed, initialize WebView
                    android.util.Log.d("MainActivity", "Initializing WebView after settings...")

                    val webViewFromXml = findViewById<WebView>(R.id.webView)
                    if (webViewFromXml == null) {
                        android.util.Log.e("MainActivity", "ERROR: WebView is null from findViewById!")
                        android.widget.Toast.makeText(this, "错误: WebView未找到", android.widget.Toast.LENGTH_LONG).show()
                        return
                    }

                    webView = webViewFromXml
                    android.util.Log.d("MainActivity", "WebView found, setting up...")
                    setupWebView()
                    registerPlaybackReceiver()
                    loadServerUrl()
                } else {
                    // User cancelled initial setup, close app
                    android.util.Log.d("MainActivity", "User cancelled setup, closing app")
                    finish()
                }
            }
            else -> {
                // Handle file chooser result
                if (::webChromeClient.isInitialized) {
                    webChromeClient.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playbackStateReceiver)
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d("MainActivity", "onResume() called")
        android.util.Log.d("MainActivity", "webView initialized: ${::webView.isInitialized}")

        // If WebView is not initialized but server is configured, initialize it now
        if (!::webView.isInitialized && !isFirstLaunch()) {
            android.util.Log.d("MainActivity", "Initializing WebView in onResume")
            webView = findViewById(R.id.webView)
            setupWebView()
            registerPlaybackReceiver()
            loadServerUrl()
        }

        if (::webView.isInitialized) {
            android.util.Log.d("MainActivity", "Calling webView.onResume()")
            webView.onResume()
        } else {
            android.util.Log.d("MainActivity", "WebView not initialized in onResume")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    companion object {
        private const val REQUEST_INITIAL_SETUP = 1000
    }
}
