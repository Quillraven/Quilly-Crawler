package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool

class RemoveComponent : Component, Pool.Poolable {
  override fun reset() = Unit
}

fun Entity.removeFromEngine(engine: Engine) {
  this.add(engine.createComponent(RemoveComponent::class.java))
}
