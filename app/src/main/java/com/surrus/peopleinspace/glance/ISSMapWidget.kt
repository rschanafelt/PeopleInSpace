package com.surrus.peopleinspace.glance

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.glance.GlanceModifier
import androidx.glance.action.actionLaunchActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ImageProvider
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.surrus.common.repository.PeopleInSpaceRepositoryInterface
import com.surrus.peopleinspace.R
import com.surrus.peopleinspace.glance.util.BaseGlanceAppWidget
import com.surrus.peopleinspace.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.overlay.IconOverlay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ISSMapWidget : BaseGlanceAppWidget<ISSMapWidget.Data>() {
    val repository: PeopleInSpaceRepositoryInterface by inject()

    data class Data(val bitmap: Bitmap?)

    override suspend fun loadData(): Data {
        val issPosition = repository.pollISSPosition().first()

        val issPositionPoint = GeoPoint(issPosition.latitude, issPosition.longitude)

        val stationMarker = IconOverlay(
            issPositionPoint,
            context.resources.getDrawable(R.drawable.ic_iss, context.theme)
        )

        val source = TileSourceFactory.DEFAULT_TILE_SOURCE
        val projection = Projection(5.0, 480, 240, issPositionPoint, 0f, true, false, 0, 0)

        val bitmap = withContext(Dispatchers.Main) {
            suspendCoroutine<Bitmap> { cont ->
                println("mapSnapshot suspendCoroutine")

//                val cache = TileWriter()

                val mapSnapshot = MapSnapshot(
                    {
                        println("mapSnapshot status " + it.status)
                        if (it.status == MapSnapshot.Status.CANVAS_OK) {
                            val bitmap = Bitmap.createBitmap(it.bitmap)
                            println("mapSnapshot returning " + bitmap.width)
                            cont.resume(bitmap)
                        }
                    },
                    MapSnapshot.INCLUDE_FLAG_UPTODATE or MapSnapshot.INCLUDE_FLAG_SCALED,
                    MapTileProviderBasic(context, source, null),
                    listOf(stationMarker),
                    projection
                )

                launch(Dispatchers.IO) {
                    println("mapSnapshot launch")
                    mapSnapshot.run()
                }
            }
        }

        println("mapSnapshot returning data " + bitmap.width)

        return Data(bitmap)
    }

    @OptIn(ExperimentalUnitApi::class)
    @Composable
    override fun Content(data: Data?) {
        Box(
            modifier = GlanceModifier.background(Color.DarkGray).fillMaxSize().clickable(
                actionLaunchActivity<MainActivity>()
            )
        ) {
            val bitmap = data?.bitmap
            if (bitmap != null) {
                androidx.glance.layout.Image(
                    modifier = GlanceModifier.fillMaxSize(),
                    provider = ImageProvider(bitmap),
                    contentDescription = "ISS Location"
                )
            } else {
                Text(
                    "Loading ISS Map...",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = TextUnit(20f, TextUnitType.Sp)
                    )
                )
            }
        }
    }
}