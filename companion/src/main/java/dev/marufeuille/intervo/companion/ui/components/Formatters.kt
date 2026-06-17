package dev.marufeuille.intervo.companion.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val historyDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d HH:mm", Locale.JAPAN).withZone(ZoneId.systemDefault())

private val detailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/M/d (E) HH:mm", Locale.JAPAN).withZone(ZoneId.systemDefault())

private val timeOnlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.JAPAN).withZone(ZoneId.systemDefault())

/** 一覧用の短い日時（例: 6/17 21:30）。 */
fun formatHistoryDate(epochMillis: Long): String =
    historyDateFormatter.format(Instant.ofEpochMilli(epochMillis))

/** 詳細用の開始〜終了レンジ（例: 2026/6/17 (火) 21:30 〜 21:54）。 */
fun formatDetailDateRange(completedAtMillis: Long, totalSeconds: Int): String {
    val end = Instant.ofEpochMilli(completedAtMillis)
    val start = end.minusSeconds(totalSeconds.toLong().coerceAtLeast(0L))
    return "${detailDateFormatter.format(start)} 〜 ${timeOnlyFormatter.format(end)}"
}

/** 所要時間（例: 24分 30秒 / 45秒）。 */
fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}分 ${seconds}秒" else "${seconds}秒"
}
