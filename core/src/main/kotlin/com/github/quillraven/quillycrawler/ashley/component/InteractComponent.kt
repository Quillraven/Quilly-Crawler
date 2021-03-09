package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxSet

class InteractComponent : Component, Pool.Poolable {
  val entitiesInRange = GdxSet<Entity>()
  var interact = false

  override fun reset() {
    entitiesInRange.clear()
    interact = false
  }

  companion object {
    val MAPPER = mapperFor<InteractComponent>()
  }
}

val Entity.interactCmp: InteractComponent
  get() = this[InteractComponent.MAPPER]
    ?: throw GdxRuntimeException("InteractComponent for entity '$this' is null")
