package com.example.mapboxleak

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.example.mapboxleak.databinding.ActivityMapboxLeakBinding
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.viewannotation.viewAnnotationOptions

class MapboxClusteringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapboxLeakBinding

    private val mapFragment
        get() = supportFragmentManager.findFragmentByTag("map")!! as ClusteringMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapboxLeakBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportFragmentManager.commitNow {
            val clusteringFragment = ClusteringMapFragment()
            this.add(binding.mapContainer.id, clusteringFragment, "map")
        }

    }

}

class ClusteringMapFragment : Fragment() {

    private val mapView
        get() = requireView() as MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = MapView(
        requireContext(),
        MapInitOptions(
            requireContext(),
            textureView = true,
            plugins = listOf(
                Plugin.Mapbox(Plugin.MAPBOX_CAMERA_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_GESTURES_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_COMPASS_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LOGO_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_ATTRIBUTION_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LIFECYCLE_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_MAP_OVERLAY_PLUGIN_ID)
            ),
            resourceOptions = MapInitOptions.getDefaultResourceOptions(requireContext())
                .toBuilder()
                .accessToken(resources.getString(R.string.mapbox_access_token))
                .build(),
            mapOptions = MapInitOptions.getDefaultMapOptions(requireContext()).toBuilder().apply {
                this.optimizeForTerrain(false)
            }.build()
        )
    ).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // if clustering is enabled one view annotation will be missing
        loadStyle(true)
    }

    @OptIn(MapboxExperimental::class)
    fun loadStyle(cluster: Boolean = true) {
        val bitmap = ContextCompat.getDrawable(requireContext(), R.drawable.ic_icon)
        mapView.getMapboxMap()
            .setCamera(CameraOptions.Builder().center(fromLngLat(0.0, 0.0)).zoom(2.0).build())
        mapView.getMapboxMap().loadStyle(
            styleExtension = style(Style.OUTDOORS) {

                +image("icon") {
                    bitmap(bitmap!!.toBitmap())
                }

                +geoJsonSource("test-source") {
                    this.clusterRadius(5)
                    this.cluster(cluster)
                    this.featureCollection(
                        FeatureCollection.fromFeatures(
                            listOf(
                                Feature.fromGeometry(
                                    fromLngLat(
                                        0.0,
                                        0.0
                                    ), null, "id-1"
                                ),
                                Feature.fromGeometry(
                                    fromLngLat(
                                        0.1,
                                        0.1
                                    ), null, "id-2"
                                )
                            )
                        )
                    )
                }

                +symbolLayer("symbol-layer", "test-source") {
                    this.iconImage("icon")
                }
            }
        )

        mapView.getMapboxMap().getStyle {

            addMarkerView("id-1", 0.0, 0.0)
            addMarkerView("id-2", 0.1, 0.1)

            mapView.getMapboxMap()
                .cameraAnimationsPlugin {
                    this.easeTo(CameraOptions.Builder().center(fromLngLat(0.05, 0.05)).zoom(10.0)
                        .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(3000)
                        }
                    )
                }
        }
    }

    @SuppressLint("SetTextI18n")
    @OptIn(MapboxExperimental::class)
    private fun addMarkerView(featureId: String, lat: Double, lon: Double) {
        val annotation = TextView(requireContext())
        annotation.text = "This is a Marker"
        annotation.background = ColorDrawable(Color.WHITE)
        annotation.setTextColor(Color.BLACK)
        annotation.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        annotation.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        annotation.layout(0, 0, annotation.measuredWidth, annotation.measuredHeight)

        mapView.viewAnnotationManager.addViewAnnotation(
            annotation,
            viewAnnotationOptions {
                this.associatedFeatureId(featureId)
                this.visible(true)
                this.width(annotation.measuredWidth)
                this.height(annotation.measuredHeight)
                this.anchor(ViewAnnotationAnchor.BOTTOM)
                this.geometry(fromLngLat(lon, lat))
                this.allowOverlap(true)
            })
    }

}