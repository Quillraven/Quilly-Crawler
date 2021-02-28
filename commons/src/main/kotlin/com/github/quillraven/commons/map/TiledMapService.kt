package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.objects.*
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import com.github.quillraven.commons.ashley.EntityConfigurations
import com.github.quillraven.commons.ashley.component.Box2DComponent
import com.github.quillraven.commons.ashley.component.Z_BACKGROUND
import com.github.quillraven.commons.ashley.component.Z_DEFAULT
import kotlinx.coroutines.launch
import ktx.ashley.entity
import ktx.ashley.with
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.box2d.*
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.tiled.property
import ktx.tiled.shape
import ktx.tiled.x
import ktx.tiled.y
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

inline fun <reified T : MapLayer> TiledMap.forEachLayer(lambda: (T) -> Unit) {
    this.layers.forEach {
        if (it::class == T::class) {
            lambda(it as T)
        }
    }
}

class TiledMapService(
    private val entityConfigurations: EntityConfigurations<out AbstractEntityConfiguration>,
    private val engine: Engine,
    assetStorage: AssetStorage,
    batch: Batch,
    private val unitScale: Float,
    private val world: World? = null,
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
            parseObjectLayers()

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

    private fun parseObjectLayers() {
        currentMap.forEachLayer<MapLayer> { layer ->
            if (layer.property("collisionLayer", false)) {
                createCollisionBody(layer.objects)
            } else {
                createEntities(layer.objects)
            }
        }
    }

    private fun createEntities(objects: MapObjects) {
        objects.forEach { mapObject ->
            entityConfigurations.newEntity(
                engine,
                mapObject.x * unitScale,
                mapObject.y * unitScale,
                mapObject.property("id"),
                world
            )
        }
    }

    private fun createCollisionBody(objects: MapObjects) {
        // TODO provide LibKTX isEmpty extension
        if (world != null && objects.count > 0) {
            engine.entity {
                with<Box2DComponent> {
                    body = world.body(BodyDef.BodyType.StaticBody) {
                        fixedRotation = true

                        objects.forEach { mapObject ->
                            val shape = mapObject.shape
                            when (shape) {
                                is Polyline -> {
                                    val x = shape.x
                                    val y = shape.y
                                    // transformed vertices also adds the position to each
                                    // vertex. Therefore, we need to set position first to ZERO
                                    // and then restore it afterwards
                                    shape.setPosition(x * unitScale, y * unitScale)
                                    shape.setScale(unitScale, unitScale)
                                    chain(shape.transformedVertices)
                                    shape.setPosition(x, y)
                                }
                                is Polygon -> {
                                    val x = shape.x
                                    val y = shape.y
                                    // transformed vertices also adds the position to each
                                    // vertex. Therefore, we need to set position first to ZERO
                                    // and then restore it afterwards
                                    shape.setPosition(x * unitScale, y * unitScale)
                                    shape.setScale(unitScale, unitScale)
                                    loop(shape.transformedVertices)
                                    shape.setPosition(x, y)
                                }
                            }
                        }

                        userData = this@entity.entity
                    }
                }
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