package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.Z_BACKGROUND
import com.github.quillraven.commons.ashley.component.Z_DEFAULT
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.tiled.property
import kotlin.math.abs
import kotlin.system.measureTimeMillis

// TODO make pull request to LibKTX
inline operator fun <reified T : MapLayer> MapLayers.invoke(fill: GdxArray<T>? = null, lambda: (T) -> Unit = {}) {
    if (fill != null) {
        this.getByType(T::class.java, fill).forEach { lambda(it) }
    } else {
        this.getByType(T::class.java).forEach { lambda(it) }
    }
}

class TiledMapService(
    assetStorage: AssetStorage,
    batch: Batch,
    unitScale: Float,
    override val mapRenderer: OrthogonalTiledMapRenderer = OrthogonalTiledMapRenderer(null, unitScale, batch)
) : MapService(assetStorage) {
    private var currentMapFilePath = ""
    private var currentMap: TiledMap = EMPTY_MAP

    private val tmpTileLayerArray = gdxArrayOf<TiledMapTileLayer>()
    private val backgroundLayers = gdxArrayOf<TiledMapTileLayer>()
    private val foregroundLayers = gdxArrayOf<TiledMapTileLayer>()

    init {
        assetStorage.setLoader<TiledMap> { TmxMapLoader(assetStorage.fileResolver) }
    }

    override fun setMap(engine: Engine, mapFilePath: String) {
        if (!assetStorage.fileResolver.resolve(mapFilePath).exists()) {
            throw GdxRuntimeException("Map '$mapFilePath' does not exist!")
        }

        KtxAsync.launch {
            if (currentMap != EMPTY_MAP) {
                assetStorage.unload<TiledMap>(currentMapFilePath)
                // TODO remove entities (maybe TiledMapObjectComponent?)
            }

            if (Gdx.app.logLevel == Application.LOG_DEBUG) {
                LOG.debug {
                    "Loading of map $mapFilePath took '${
                        measureTimeMillis {
                            currentMap = assetStorage.loadSync(mapFilePath)
                        }
                    }' ms"
                }
            } else {
                currentMap = assetStorage.loadSync(mapFilePath)
            }

            parseRenderLayers()

            currentMapFilePath = mapFilePath
            mapRenderer.map = currentMap
            listeners.forEach { it.onMapChange(this@TiledMapService, currentMap) }
        }
    }

    private fun parseRenderLayers() {
        backgroundLayers.clear()
        foregroundLayers.clear()
        currentMap.layers(tmpTileLayerArray) { layer ->
            if (layer.property(Z_PROPERTY, Z_BACKGROUND) <= Z_DEFAULT) {
                backgroundLayers.add(layer)
            } else {
                foregroundLayers.add(layer)
            }
        }
    }

    override fun forEachMapObject(lambda: (MapObject) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun setViewBounds(camera: OrthographicCamera) {
        val width: Float = camera.viewportWidth * camera.zoom
        val height: Float = camera.viewportHeight * camera.zoom
        val w: Float = width * abs(camera.up.y) + height * abs(camera.up.x)
        val h: Float = height * abs(camera.up.y) + width * abs(camera.up.x)
        mapRenderer.viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
    }

    override fun renderBackground() {
        backgroundLayers.forEach { mapRenderer.renderTileLayer(it) }
    }

    override fun renderForeground() {
        foregroundLayers.forEach { mapRenderer.renderTileLayer(it) }
    }

    companion object {
        const val Z_PROPERTY = "z"
        private val EMPTY_MAP: TiledMap = TiledMap()
    }
}