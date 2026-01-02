package com.netdisk.app.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.netdisk.app.storage.PreferencesManager

class NetdiskWebViewClient(
    private val preferencesManager: PreferencesManager
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started loading: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished loading: $url")

        // Save authentication cookies if user successfully logged in
        if (url?.contains("/browse") == true || url?.contains("/") == true) {
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null && cookies.isNotEmpty()) {
                preferencesManager.saveAuthCookies(cookies)
                Log.d(TAG, "Saved authentication cookies")

                // 获取流媒体访问 token
                fetchStreamToken(view)
            }
        }
    }

    private fun fetchStreamToken(webView: WebView?) {
        val serverUrl = preferencesManager.getServerUrl()
        val tokenApiUrl = "$serverUrl/api/get_stream_token"

        // 使用 JavaScript fetch API 获取 token
        val script = """
            console.log('开始获取 token, URL: $tokenApiUrl');
            fetch('$tokenApiUrl', { credentials: 'include' })
                .then(response => {
                    console.log('Token API 响应状态:', response.status);
                    return response.json();
                })
                .then(data => {
                    console.log('Token API 返回数据:', JSON.stringify(data));
                    if (data.success && data.token) {
                        console.log('调用 Android.saveStreamToken, token长度:', data.token.length);
                        Android.saveStreamToken(data.token);
                        console.log('Token 保存完成');
                    } else {
                        console.error('Token 数据无效:', data);
                    }
                })
                .catch(error => {
                    console.error('获取 token 失败:', error.toString());
                });
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
        Log.d(TAG, "Fetching stream token from: $tokenApiUrl")
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        // Let WebView handle all URLs (stay within the app)
        Log.d(TAG, "shouldOverrideUrlLoading: $url")
        return false
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        Log.e(TAG, "onReceivedError: code=$errorCode, description=$description, url=$failingUrl")
    }

    companion object {
        private const val TAG = "NetdiskWebViewClient"
    }
}
