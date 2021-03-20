package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray

class ConsumeComponent : Component, Pool.Poolable {
  val itemsToConsume = GdxArray<Entity>()

  override fun reset() {
    itemsToConsume.clear()
  }

  companion object {
    val MAPPER = mapperFor<ConsumeComponent>()
  }
}

val Entity.consumeCmp: ConsumeComponent
  get() = this[ConsumeComponent.MAPPER]
    ?: throw GdxRuntimeException("ConsumeComponent for entity '$this' is null")
