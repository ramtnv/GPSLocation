package com.greetings.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

private const val YOUTUBE_SHORTS_URL = "https://www.youtube.com/shorts"
private val Background = Color(0xFF101014)
private val SurfaceColor = Color(0xFF1D1D24)
private val Accent = Color(0xFFFF3347)
private val TextPrimary = Color(0xFFF7F5FA)
private val TextSecondary = Color(0xFFB7B4C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearLegacyGreetingAndTrackingData()
        deleteSharedPreferences("shorts_location_sync")
        setContent {
            ShortsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    ShortsApp()
                }
            }
        }
    }

    private fun clearLegacyGreetingAndTrackingData() {
        val migration = getSharedPreferences("shorts_migration", MODE_PRIVATE)
        if (migration.getBoolean("legacy_data_cleared", false)) return

        val profileCleared = getSharedPreferences("greetings_profile", MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val trackingCleared = getSharedPreferences("greetings_location_sharing", MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val historyDatabase = getDatabasePath("location_history.db")
        val historyCleared = !historyDatabase.exists() || deleteDatabase("location_history.db")

        if (profileCleared && trackingCleared && historyCleared) {
            migration.edit().putBoolean("legacy_data_cleared", true).apply()
        } else {
            Log.e(
                "ShortsMigration",
                "Could not clear all data from the previous Greetings app version.",
            )
        }

    }
}

@Composable
private fun ShortsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            background = Background,
            surface = SurfaceColor,
            onSurface = TextPrimary,
        ),
        content = content,
    )
}

@Composable
private fun ShortsApp() {
    var showLocationSharing by remember { mutableStateOf(false) }

    BackHandler(enabled = showLocationSharing) {
        showLocationSharing = false
    }

    if (showLocationSharing) {
        LocationEmailScreen(onBack = { showLocationSharing = false })
    } else {
        ShortsHome(onOpenEmailLocation = { showLocationSharing = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortsHome(onOpenEmailLocation: () -> Unit) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 22.dp)
                                .background(Accent, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("▶", fontSize = 12.sp, color = Color.White)
                        }
                        Spacer(Modifier.size(10.dp))
                        Text("Shorts", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = onOpenEmailLocation) {
                        Text(
                            "Login for gifts",
                            color = TextPrimary,
                            fontSize = 13.sp,
                        )
                    }
                },
            )
        },
    ) { padding ->
        ShortsWebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShortsWebView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            request.deny()
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String,
                            callback: android.webkit.GeolocationPermissions.Callback,
                        ) {
                            callback.invoke(origin, false, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                            pageError = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                pageError = "Could not load YouTube Shorts. Check your internet connection and try again."
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
                            if (uri.scheme == "https" && isYouTubeWebHost(host)) return false
                            if (uri.scheme == "https" || uri.scheme == "http") {
                                return try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    true
                                } catch (_: ActivityNotFoundException) {
                                    pageError = "No browser is available to open this link."
                                    true
                                }
                            }
                            return true
                        }
                    }
                    webView = this
                    loadUrl(YOUTUBE_SHORTS_URL)
                }
            },
            update = { webView = it },
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Accent,
                trackColor = Color.Transparent,
            )
        }

        if (pageError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(SurfaceColor, RoundedCornerShape(20.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(pageError.orEmpty(), color = TextPrimary, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        pageError = null
                        isLoading = true
                        webView?.loadUrl(YOUTUBE_SHORTS_URL)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text("Try again")
                }
            }
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

private fun isYouTubeWebHost(host: String): Boolean =
    host == "youtube.com" ||
        host.endsWith(".youtube.com") ||
        host == "youtube-nocookie.com" ||
        host.endsWith(".youtube-nocookie.com")
