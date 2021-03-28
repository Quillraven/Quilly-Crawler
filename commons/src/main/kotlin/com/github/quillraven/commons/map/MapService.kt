package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import com.github.quillraven.commons.ashley.system.RenderSystem
import ktx.log.logger

/**
 * Interface for 2D-map specific services like tiled maps. A [MapService] is used by the
 * [RenderSystem] to render the map by calling the [setViewBounds], [renderBackground] and [renderForeground]
 * functions of the map service implementation.
 *
 * Refer to [TiledMapService] for an example implementation.
 */
interface MapService {
  fun setMap(engine: Engine, mapFilePath: String)

  fun setViewBounds(camera: OrthographicCamera)

  fun renderBackground()

  fun renderForeground()

  companion object {
    val LOG = logger<MapService>()
  }
}

/**
 * Empty implementation of [MapService]. Can be used as default value to avoid null services.
 */
object DefaultMapService : MapService {
  override fun setMap(engine: Engine, mapFilePath: String) = Unit

  override fun setViewBounds(camera: OrthographicCamera) = Unit

  override fun renderBackground() = Unit

  override fun renderForeground() = Unit
}
