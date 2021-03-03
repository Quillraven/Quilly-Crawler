package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxSet

class CollisionComponent : Component, Pool.Poolable {
  val beginContactEntities = GdxSet<Entity>()
  val endContactEntities = GdxSet<Entity>()

  override fun reset() {
    beginContactEntities.clear()
    endContactEntities.clear()
  }

  companion object {
    val MAPPER = mapperFor<CollisionComponent>()
  }
}

val Entity.collisionCmp: CollisionComponent
  get() = this[CollisionComponent.MAPPER]
    ?: throw GdxRuntimeException("CollisionComponent for entity '$this' is null")
