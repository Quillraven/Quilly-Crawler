package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

class CollectableComponent : Component, Pool.Poolable {

  override fun reset() {
  }

  companion object {
    val MAPPER = mapperFor<CollectableComponent>()
  }
}
