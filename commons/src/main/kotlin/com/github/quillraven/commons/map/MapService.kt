package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.Map
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.GdxSet
import ktx.log.error
import ktx.log.logger

interface MapListener {
    fun onMapChange(mapService: MapService, map: Map)
}

abstract class MapService(
    protected val assetStorage: AssetStorage
) {
    protected val listeners = GdxSet<MapListener>()
    abstract val mapRenderer: OrthogonalTiledMapRenderer

    init {
        KtxAsync.launch {
            assetStorage.add("mapServiceMapRenderer", mapRenderer)
        }
    }

    fun addMapListener(listener: MapListener) {
        if (!listeners.add(listener)) {
            LOG.error { "Trying to add MapListener $listener multiple times" }
        }
    }

    fun removeMapListener(listener: MapListener) {
        if (!listeners.remove(listener)) {
            LOG.error { "MapListener $listener was not registered yet" }
        }
    }

    abstract fun setMap(engine: Engine, mapFilePath: String)

    abstract fun forEachMapObject(lambda: (MapObject) -> Unit)

    abstract fun setViewBounds(camera: OrthographicCamera)

    abstract fun renderBackground()

    abstract fun renderForeground()

    companion object {
        val LOG = logger<MapService>()
    }
}