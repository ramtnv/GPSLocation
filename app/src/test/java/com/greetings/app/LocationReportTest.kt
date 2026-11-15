package com.greetings.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LocationReportTest {
    @Test
    fun emailBodyIncludesDeviceLocationAccuracyAndTimestamp() {
        val body = LocationReport(
            latitude = 13.0414,
            longitude = 80.1267,
            accuracyMeters = 8f,
            timestamp = Instant.parse("2026-11-15T07:30:45Z"),
        ).emailBody("Example device (Android 16)")

        assertTrue(body.contains("Device details: Example device (Android 16)"))
        assertTrue(body.contains("Location: 13.041400, 80.126700"))
        assertTrue(body.contains("Accuracy: about 8 m"))
        assertTrue(body.contains("Timestamp: "))
    }

    @Test
    fun shareTextContainsCoordinatesAccuracyAndMapsLink() {
        val text = LocationReport(
            latitude = 13.0414,
            longitude = 80.1267,
            accuracyMeters = 8f,
            timestamp = Instant.parse("2026-11-15T07:30:45Z"),
        ).shareText()

        assertTrue(text.contains("13.041400, 80.126700"))
        assertTrue(text.contains("Accuracy: about 8 m"))
        assertTrue(text.contains("https://maps.google.com/?q=13.041400,80.126700"))
    }
}
