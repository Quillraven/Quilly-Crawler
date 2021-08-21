package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

class CombatLootComponent : Component, Pool.Poolable {
  var victory = false
  var gold = 0

  override fun reset() {
    victory = false
    gold = 0
  }

  companion object {
    val MAPPER = mapperFor<CombatLootComponent>()
  }
}

val Entity.combatLootCmp: CombatLootComponent
  get() = this[CombatLootComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatLootComponent for entity '$this' is null")
