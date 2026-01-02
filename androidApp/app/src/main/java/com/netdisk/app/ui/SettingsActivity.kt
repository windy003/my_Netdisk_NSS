package com.netdisk.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.netdisk.app.R
import com.netdisk.app.storage.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferencesManager = PreferencesManager(this)

        // Initialize views
        serverUrlInput = findViewById(R.id.serverUrlInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)

        // Load current settings
        loadSettings()

        // Save button click listener
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        serverUrlInput.setText(preferencesManager.getServerHost())
        portInput.setText(preferencesManager.getServerPort().toString())
    }

    private fun saveSettings() {
        var host = serverUrlInput.text.toString().trim()
        val portStr = portInput.text.toString().trim()

        if (host.isEmpty()) {
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // 自动提取主机名（支持输入完整 URL）
        host = extractHost(host)
        serverUrlInput.setText(host)

        if (portStr.isEmpty()) {
            Toast.makeText(this, "Port cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val port = portStr.toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate URL format (basic validation)
        if (!isValidHost(host)) {
            Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            return
        }

        // Save settings
        preferencesManager.saveServerConfig(host, port)
        Toast.makeText(this, getString(R.string.server_config_saved), Toast.LENGTH_SHORT).show()

        // Set result and go back to main activity
        setResult(RESULT_OK)
        finish()
    }

    private fun extractHost(input: String): String {
        var host = input
        // 移除 http:// 或 https:// 前缀
        if (host.startsWith("http://")) {
            host = host.substring(7)
        } else if (host.startsWith("https://")) {
            host = host.substring(8)
        }
        // 移除路径部分
        val slashIndex = host.indexOf('/')
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex)
        }
        // 移除端口部分
        val colonIndex = host.indexOf(':')
        if (colonIndex > 0) {
            host = host.substring(0, colonIndex)
        }
        return host
    }

    private fun isValidHost(host: String): Boolean {
        // Simple validation - check if it's an IP address or domain name
        val ipPattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        val domainPattern = Regex("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")

        return host == "localhost" || ipPattern.matches(host) || domainPattern.matches(host)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
