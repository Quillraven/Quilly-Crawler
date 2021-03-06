package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.RemoveSystem

/**
 * Component to mark an [Entity] for removal. It is used by the [RemoveSystem] to
 * remove entities from the [Engine] at the beginning/end of a frame.
 */
class RemoveComponent : Component, Pool.Poolable {
  override fun reset() = Unit
}

/**
 * Adds a [RemoveComponent] to the [Entity] by using the [engine's][Engine]
 * [createComponent][Engine.createComponent] method.
 */
fun Entity.removeFromEngine(engine: Engine) {
  this.add(engine.createComponent(RemoveComponent::class.java))
}
