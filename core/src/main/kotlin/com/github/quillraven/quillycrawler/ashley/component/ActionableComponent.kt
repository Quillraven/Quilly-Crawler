package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class ActionableComponent : Component, Pool.Poolable {
  var isExit = false

  override fun reset() {
    isExit = false
  }

  companion object {
    val MAPPER = mapperFor<ActionableComponent>()
  }
}
