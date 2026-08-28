package com.charlztech.charlztechtv.util

import android.util.Base64
import com.charlztech.charlztechtv.BuildConfig
import com.charlztech.charlztechtv.data.model.EventStatus
import com.charlztech.charlztechtv.data.model.LiveEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private val keys: List<KeyInfo> by lazy {
        listOfNotNull(
            BuildConfig.CRYPTO_SECRET1.takeIf { it.isNotBlank() }?.let { parseKeyInfo(it) },
            BuildConfig.CRYPTO_SECRET2.takeIf { it.isNotBlank() }?.let { parseKeyInfo(it) }
        )
    }

    private data class KeyInfo(val key: ByteArray, val iv: ByteArray)

    fun decryptData(encryptedBase64: String): String? {
        if (keys.isEmpty()) return null
        return try {
            val clean = encryptedBase64.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .replace("\t", "")
            val ciphertext = Base64.decode(clean, Base64.DEFAULT)
            keys.firstNotNullOfOrNull { tryDecrypt(ciphertext, it) }
        } catch (_: Exception) {
            null
        }
    }

    fun decryptContent(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("#EXTM3U") ||
            trimmed.startsWith("#EXTINF") ||
            trimmed.startsWith("#KODIPROP") ||
            trimmed.startsWith("[") ||
            trimmed.startsWith("{")
        ) {
            return trimmed
        }
        if (trimmed.length < 79) return trimmed
        return try {
            val part1 = trimmed.substring(0, 10)
            val part2 = trimmed.substring(34, trimmed.length - 54)
            val part3 = trimmed.substring(trimmed.length - 10)
            val encryptedDataStr = part1 + part2 + part3
            val iv = Base64.decode(trimmed.substring(10, 34), Base64.DEFAULT)
            val key = Base64.decode(trimmed.substring(trimmed.length - 54, trimmed.length - 10), Base64.DEFAULT)
            val encryptedBytes = Base64.decode(encryptedDataStr, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            decryptData(trimmed) ?: trimmed
        }
    }

    private fun parseKeyInfo(secret: String): KeyInfo {
        val parts = secret.split(":")
        return KeyInfo(hexToBytes(parts[0]), hexToBytes(parts[1]))
    }

    private fun hexToBytes(hex: String): ByteArray {
        val data = ByteArray(hex.length / 2)
        for (i in data.indices) {
            data[i] = ((Character.digit(hex[i * 2], 16) shl 4) +
                Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return data
    }

    private fun tryDecrypt(ciphertext: ByteArray, keyInfo: KeyInfo): String? {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyInfo.key, "AES"),
                IvParameterSpec(keyInfo.iv)
            )
            val text = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            if (text.startsWith("{") || text.startsWith("[") || text.contains("http", ignoreCase = true)) {
                text
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

object EventStatusUtils {
    private val apiFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US)
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault())

    fun parseStartTime(event: LiveEvent): Long? = parseTime(event.eventInfo?.startTime)

    fun parseEndTime(event: LiveEvent): Long? = parseTime(event.eventInfo?.endTime)

    private fun parseTime(raw: String?): Long? = try {
        raw?.let { apiFormat.parse(it)?.time }
    } catch (_: Exception) {
        null
    }

    fun formatScheduleLabel(event: LiveEvent): String? {
        val start = parseStartTime(event) ?: return null
        return displayDateTimeFormat.format(Date(start))
    }

    fun formatScheduleDetail(event: LiveEvent, status: EventStatus): String? {
        val start = parseStartTime(event) ?: return null
        val end = parseEndTime(event)
        val now = System.currentTimeMillis()
        return when (status) {
            EventStatus.UPCOMING -> formatCountdown(start - now)?.let { "Starts $it" }
            EventStatus.LIVE -> {
                val started = displayTimeFormat.format(Date(start))
                if (end != null && end > now) {
                    val endsIn = formatCountdown(end - now)
                    if (endsIn != null) "Started $started · Ends in $endsIn" else "Started $started"
                } else {
                    "Started $started"
                }
            }
            EventStatus.ENDED -> {
                val endMs = end ?: start
                "Ended ${displayDateTimeFormat.format(Date(endMs))}"
            }
            EventStatus.UNKNOWN -> displayDateTimeFormat.format(Date(start))
        }
    }

    private fun formatCountdown(millis: Long): String? {
        if (millis <= 0) return null
        val totalMinutes = millis / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "in ${days}d ${hours}h"
            hours > 0 -> "in ${hours}h ${minutes}m"
            minutes > 0 -> "in ${minutes}m"
            else -> "in <1m"
        }
    }

    fun getStatus(event: LiveEvent): EventStatus {
        val info = event.eventInfo ?: return EventStatus.UNKNOWN
        val now = System.currentTimeMillis()
        return try {
            val start = info.startTime?.let { apiFormat.parse(it)?.time }
            val end = info.endTime?.let { apiFormat.parse(it)?.time }
            when {
                end != null && now >= end -> EventStatus.ENDED
                start != null && now >= start -> EventStatus.LIVE
                start != null -> EventStatus.UPCOMING
                else -> EventStatus.UNKNOWN
            }
        } catch (_: Exception) {
            EventStatus.UNKNOWN
        }
    }

    fun displayTitle(event: LiveEvent): String {
        val info = event.eventInfo
        return if (!info?.teamA.isNullOrBlank() && !info?.teamB.isNullOrBlank()) {
            if (info.teamA == info.teamB) info.teamA!! else "${info.teamA} vs ${info.teamB}"
        } else {
            event.title
        }
    }

    fun category(event: LiveEvent): String =
        event.eventInfo?.eventCat ?: event.cat ?: "Other"

    fun matchCardUrl(event: LiveEvent, status: EventStatus): String {
        val info = event.eventInfo
        val title = java.net.URLEncoder.encode(info?.eventName ?: event.title, "UTF-8")
        val teamA = java.net.URLEncoder.encode(info?.teamA ?: "Team A", "UTF-8")
        val teamB = java.net.URLEncoder.encode(info?.teamB ?: "Team B", "UTF-8")
        val isLive = status == EventStatus.LIVE
        val isEnded = status == EventStatus.ENDED
        return buildString {
            append("https://live-card-png.cricify.workers.dev/?")
            append("title=$title&teamA=$teamA&teamB=$teamB")
            info?.teamAFlag?.takeIf { it.isNotBlank() }?.let { append("&teamAImg=$it") }
            info?.teamBFlag?.takeIf { it.isNotBlank() }?.let { append("&teamBImg=$it") }
            info?.eventLogo?.takeIf { it.isNotBlank() }?.let { append("&eventLogo=$it") }
            append("&isLive=$isLive&isEnded=$isEnded")
        }
    }
}

object StreamLinkParser {
    fun parse(link: String): Pair<String, Map<String, String>> {
        if (!link.contains("|")) return link to emptyMap()
        val parts = link.split("|", limit = 2)
        val url = parts[0]
        val headers = mutableMapOf<String, String>()
        if (parts.size > 1) {
            parts[1].split("&").forEach { param ->
                val kv = param.split("=", limit = 2)
                if (kv.size == 2) {
                    headers[kv[0].trim()] = kv[1].trim()
                }
            }
        }
        return url to headers
    }

    fun isDirectStream(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains(".mpd") ||
            lower.contains(".mp4") || lower.contains(".ts") ||
            lower.contains(".mkv") || lower.contains(".webm")
    }
}
