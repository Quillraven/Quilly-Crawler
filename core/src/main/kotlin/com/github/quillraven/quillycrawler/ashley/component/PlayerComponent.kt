package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to identify a player [Entity]. Can be used for [iterating systems][IteratingSystem] to
 * only run them for player entities.
 */
class PlayerComponent : Component, Pool.Poolable {
  var dungeonLevel = 0

  override fun reset() {
    dungeonLevel = 0
  }

  companion object {
    val MAPPER = mapperFor<PlayerComponent>()
  }
}

val Entity.playerCmp: PlayerComponent
  get() = this[PlayerComponent.MAPPER]
    ?: throw GdxRuntimeException("PlayerComponent for entity '$this' is null")

val Entity.isPlayer: Boolean
  get() = this[PlayerComponent.MAPPER] != null
