package dev.marufeuille.intervo.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import dev.marufeuille.intervo.MainActivity
import dev.marufeuille.intervo.R
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 「タップで Intervo を起動」するシンプルなコンプリケーション。
 * SHORT_TEXT では登録済みワークアウト数を表示し、MONOCHROMATIC_IMAGE ではアイコンを表示する。
 */
class LaunchComplicationService : ComplicationDataSourceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = buildData(type, count = 3)

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        scope.launch {
            val count = runCatching {
                WorkoutRepository(AppDatabase.getInstance(applicationContext)).workoutsWithCount.first().size
            }.getOrDefault(0)
            listener.onComplicationData(buildData(request.complicationType, count))
        }
    }

    private fun buildData(type: ComplicationType, count: Int): ComplicationData? {
        val description = PlainComplicationText.Builder(CONTENT_DESCRIPTION).build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(count.toString()).build(),
                contentDescription = description,
            )
                .setMonochromaticImage(icon())
                .setTapAction(launchIntent())
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon(),
                contentDescription = description,
            )
                .setTapAction(launchIntent())
                .build()

            else -> null
        }
    }

    private fun icon(): MonochromaticImage =
        MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification_timer)
        ).build()

    private fun launchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val CONTENT_DESCRIPTION = "Intervo を開く"
    }
}
