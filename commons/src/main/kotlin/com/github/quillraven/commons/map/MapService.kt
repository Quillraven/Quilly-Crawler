package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import ktx.log.logger

interface MapService {
  fun setMap(engine: Engine, mapFilePath: String)

  fun setViewBounds(camera: OrthographicCamera)

  fun renderBackground()

  fun renderForeground()

  companion object {
    val LOG = logger<MapService>()
  }
}

class DefaultMapService : MapService {
  override fun setMap(engine: Engine, mapFilePath: String) = Unit

  override fun setViewBounds(camera: OrthographicCamera) = Unit

  override fun renderBackground() = Unit

  override fun renderForeground() = Unit
}
