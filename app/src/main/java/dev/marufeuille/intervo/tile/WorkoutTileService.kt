package dev.marufeuille.intervo.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dev.marufeuille.intervo.EXTRA_WORKOUT_ID
import dev.marufeuille.intervo.MainActivity
import dev.marufeuille.intervo.data.AppDatabase
import dev.marufeuille.intervo.data.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"
private const val ORANGE = 0xFFFF6B2C.toInt()
private const val SURFACE = 0xFF1C1C1C.toInt()
private const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
private const val TEXT_SECONDARY = 0xFF9E9E9E.toInt()

/**
 * ウォッチフェイスの Tile に登録済みワークアウトをチップ表示する。
 * チップをタップすると MainActivity を起動し、そのワークアウトの詳細画面へ遷移する。
 */
class WorkoutTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val repo = WorkoutRepository(AppDatabase.getInstance(applicationContext))
        val workouts = tileWorkouts(repo.workoutsWithCount.first())
        val layout = tileLayout(this@WorkoutTileService, requestParams.deviceConfiguration, workouts)
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    private fun tileLayout(
        context: Context,
        device: DeviceParameters,
        workouts: List<TileWorkout>,
    ): LayoutElement {
        val column = LayoutElementBuilders.Column.Builder().setWidth(expand())
        if (workouts.isEmpty()) {
            column.addContent(
                chip(context, device, "＋ アプリで追加", launchClickable(null), ORANGE)
            )
        } else {
            workouts.forEachIndexed { index, w ->
                if (index > 0) {
                    column.addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(androidx.wear.protolayout.DimensionBuilders.dp(6f))
                            .build()
                    )
                }
                column.addContent(chip(context, device, w.name, launchClickable(w.id), SURFACE))
            }
        }
        return PrimaryLayout.Builder(device)
            .setResponsiveContentInsetEnabled(true)
            .setPrimaryLabelTextContent(
                Text.Builder(context, "ワークアウト")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(androidx.wear.protolayout.ColorBuilders.argb(TEXT_SECONDARY))
                    .build()
            )
            .setContent(column.build())
            .build()
    }

    private fun chip(
        context: Context,
        device: DeviceParameters,
        label: String,
        clickable: ModifiersBuilders.Clickable,
        background: Int,
    ): LayoutElement =
        Chip.Builder(context, clickable, device)
            .setPrimaryLabelContent(label)
            .setWidth(expand())
            .setChipColors(
                ChipColors(
                    /* backgroundColor = */ androidx.wear.protolayout.ColorBuilders.argb(background),
                    /* contentColor = */ androidx.wear.protolayout.ColorBuilders.argb(TEXT_PRIMARY)
                )
            )
            .build()

    /** workoutId が null のときは extra なし（一覧画面が開く） */
    private fun launchClickable(workoutId: String?): ModifiersBuilders.Clickable {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(MainActivity::class.java.name)
            .apply {
                if (workoutId != null) {
                    addKeyToExtraMapping(
                        EXTRA_WORKOUT_ID,
                        ActionBuilders.AndroidStringExtra.Builder().setValue(workoutId).build()
                    )
                }
            }
            .build()
        return ModifiersBuilders.Clickable.Builder()
            .setId(workoutId ?: "add")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity).build())
            .build()
    }
}
