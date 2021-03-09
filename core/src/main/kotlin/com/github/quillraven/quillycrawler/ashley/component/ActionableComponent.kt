package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class ActionType {
  UNDEFINED, EXIT, CHEST
}

class ActionableComponent : Component, Pool.Poolable {
  var type = ActionType.UNDEFINED

  override fun reset() {
    type = ActionType.UNDEFINED
  }

  companion object {
    val MAPPER = mapperFor<ActionableComponent>()
  }
}

val Entity.actionableCmp: ActionableComponent
  get() = this[ActionableComponent.MAPPER]
    ?: throw GdxRuntimeException("ActionableComponent for entity '$this' is null")
