package com.example.smartcctv

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FullScreenWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_web_view)

        // Sembunyikan System UI (Status Bar & Navigation Bar) untuk fullscreen
        hideSystemUI()

        val webView: WebView = findViewById(R.id.fullscreen_webview)
        val url = intent.getStringExtra("EXTRA_URL") // Ambil URL yang dikirim dari MainActivity

        webView.settings.javaScriptEnabled = true
        if (url != null) {
            webView.loadUrl(url)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
}