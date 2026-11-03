package com.hoan.ytcar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * YT Car 2.0
 *
 * Passenger Mode is an in-app UI mode on the phone/app display. It does not
 * project video into Android Auto or bypass OEM/vehicle motion restrictions.
 * Background audio is supported for user-selected local/authorized audio files
 * through an Android Media3 foreground media service.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var statusText: TextView
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var passengerMode = true

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selected ->
            try {
                contentResolver.takePersistableUriPermission(
                    selected,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Temporary read permission may still be valid for this playback session.
            }
            val playIntent = Intent(this, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_PLAY
                data = selected
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ContextCompat.startForegroundService(this, playIntent)
            statusText.text = "Đang phát âm thanh nền từ tệp đã chọn"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: android.webkit.WebResourceRequest
                ): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    return scheme != "https" && scheme != "http"
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    callback?.invoke(origin, false, false)
                }

                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    exitFullscreen()
                    fullscreenView = view
                    fullscreenCallback = callback
                    root.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    topBar.visibility = View.GONE
                    bottomBar.visibility = View.GONE
                    statusText.visibility = View.GONE
                    web.visibility = View.GONE
                }

                override fun onHideCustomView() = exitFullscreen()
            }
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }

        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(20, 25, 33))
            addView(makeButton("YT Car") { web.loadUrl("https://m.youtube.com/") },
                LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(makeButton("Passenger Mode") { togglePassengerMode() },
                LinearLayout.LayoutParams(0, dp(48), 1.4f))
        }

        statusText = TextView(this).apply {
            text = "Chế độ thường • YouTube trong YT Car"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            setBackgroundColor(Color.rgb(20, 25, 33))
        }

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(20, 25, 33))
            addView(makeButton("Trang chủ") { web.loadUrl("https://m.youtube.com/") },
                LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(makeButton("Quay lại") {
                if (web.canGoBack()) web.goBack() else web.loadUrl("https://m.youtube.com/")
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(makeButton("Âm thanh nền") {
                pickAudio.launch(arrayOf("audio/*"))
            }, LinearLayout.LayoutParams(0, dp(48), 1.2f))
            addView(makeButton("YouTube Music") {
                openAppOrWeb(
                    "com.google.android.apps.youtube.music",
                    "https://music.youtube.com/"
                )
            }, LinearLayout.LayoutParams(0, dp(48), 1.2f))
        }

        root.addView(web, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(topBar, FrameLayout.LayoutParams(MATCH_PARENT, dp(56), android.view.Gravity.TOP))
        root.addView(statusText, FrameLayout.LayoutParams(MATCH_PARENT, dp(30), android.view.Gravity.TOP).apply {
            topMargin = dp(56)
        })
        root.addView(bottomBar, FrameLayout.LayoutParams(MATCH_PARENT, dp(48), android.view.Gravity.BOTTOM))
        web.setPadding(0, dp(86), 0, dp(48))
        web.clipToPadding = false
        setContentView(root)

        if (savedInstanceState != null) {
            if (web.restoreState(savedInstanceState) == null) web.loadUrl("https://m.youtube.com/")
            passengerMode = savedInstanceState.getBoolean(KEY_PASSENGER_MODE, true)
            applyPassengerMode()
        } else {
            // Passenger Mode is enabled automatically on every fresh app launch.
            applyPassengerMode()
            web.loadUrl("https://m.youtube.com/")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullscreenView != null -> exitFullscreen()
                    web.canGoBack() -> web.goBack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun togglePassengerMode() {
        passengerMode = !passengerMode
        applyPassengerMode()
    }

    private fun applyPassengerMode() {
        if (passengerMode) {
            statusText.text = "PASSENGER MODE • Giao diện hành khách trong YT Car"
            topBar.setBackgroundColor(Color.rgb(16, 72, 92))
            bottomBar.setBackgroundColor(Color.rgb(16, 72, 92))
            statusText.setBackgroundColor(Color.rgb(16, 72, 92))
            web.setPadding(0, dp(86), 0, dp(48))
        } else {
            statusText.text = "Chế độ thường • YouTube trong YT Car"
            topBar.setBackgroundColor(Color.rgb(20, 25, 33))
            bottomBar.setBackgroundColor(Color.rgb(20, 25, 33))
            statusText.setBackgroundColor(Color.rgb(20, 25, 33))
            web.setPadding(0, dp(86), 0, dp(48))
        }
        web.requestLayout()
    }

    private fun openAppOrWeb(packageName: String, url: String) {
        val uri = Uri.parse(url)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { action() }
        }

    private fun exitFullscreen() {
        val view = fullscreenView ?: return
        root.removeView(view)
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        web.visibility = View.VISIBLE
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PASSENGER_MODE, passengerMode)
        web.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        // The WebView does not continue hidden video playback. The independent
        // Media3 service handles only user-selected audio files.
        web.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.loadUrl("about:blank")
            web.removeAllViews()
            web.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val KEY_PASSENGER_MODE = "passenger_mode"
    }
}
