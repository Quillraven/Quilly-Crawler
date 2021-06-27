package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class ActionType {
  UNDEFINED, EXIT, CHEST, ENEMY, SHOP, REAPER
}

class ActionableComponent : Component, Pool.Poolable {
  var type = ActionType.UNDEFINED
  var outlineColor = Color(1f, 1f, 1f, 1f)

  override fun reset() {
    type = ActionType.UNDEFINED
    outlineColor.set(1f, 1f, 1f, 1f)
  }

  companion object {
    val MAPPER = mapperFor<ActionableComponent>()
  }
}

val Entity.actionableCmp: ActionableComponent
  get() = this[ActionableComponent.MAPPER]
    ?: throw GdxRuntimeException("ActionableComponent for entity '$this' is null")
