package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.Map
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.GdxSet
import ktx.log.error
import ktx.log.logger

interface MapListener {
  fun beforeMapChange(mapService: MapService, map: Map)

  fun afterMapChange(mapService: MapService, map: Map)

  fun onMapEntityCreation(entity: Entity)
}

abstract class MapService(
  val assetStorage: AssetStorage,
  val engine: Engine
) {
  protected val listeners = GdxSet<MapListener>()
  abstract val mapRenderer: OrthogonalTiledMapRenderer
  abstract val mapEntities: ImmutableArray<Entity>

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

  abstract fun setViewBounds(camera: OrthographicCamera)

  abstract fun renderBackground()

  abstract fun renderForeground()

  companion object {
    val LOG = logger<MapService>()
  }
}
