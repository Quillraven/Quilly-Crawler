package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor
import ktx.collections.GdxSet

class CollectingComponent : Component, Pool.Poolable {
  val entitiesInRange = GdxSet<Entity>()

  override fun reset() {
    entitiesInRange.clear()
  }

  companion object {
    val MAPPER = mapperFor<CollectingComponent>()
  }
}
