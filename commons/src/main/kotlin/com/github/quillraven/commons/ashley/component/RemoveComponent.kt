package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.RemoveSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to mark an [Entity] for removal. It is used by the [RemoveSystem] to
 * remove entities from the [Engine] at the beginning/end of a frame.
 *
 * Use [delay] to remove an entity after a specific amount of seconds.
 */
class RemoveComponent : Component, Pool.Poolable {
  var delay = 0f

  override fun reset() {
    delay = 0f
  }

  companion object {
    val MAPPER = mapperFor<RemoveComponent>()
  }
}

/**
 * Returns a [FadeComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.removeCmp: RemoveComponent
  get() = this[RemoveComponent.MAPPER]
    ?: throw GdxRuntimeException("RemoveComponent for entity '$this' is null")

/**
 * Adds a [RemoveComponent] to the [Entity] by using the [engine's][Engine]
 * [createComponent][Engine.createComponent] method.
 * The entity then gets removed by the [RemoveSystem].
 *
 * Use [delay] to remove the entity after a specific amount of seconds.
 */
fun Entity.removeFromEngine(engine: Engine, delay: Float = 0f) {
  this.add(engine.createComponent(RemoveComponent::class.java).apply { this.delay = delay })
}

/**
 * Checks if an [Entity] gets removed by the engine or has a [RemoveComponent].
 * Use this instead of [Entity.isRemoving] to check if an entity really gets removed.
 */
val Entity.isRemoved: Boolean
  get() {
    return this.isRemoving || this[RemoveComponent.MAPPER] != null
  }
