package com.github.quillraven.quillycrawler.combat.buff

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.event.GameEventListener
import ktx.log.logger

sealed class Buff(
  context: CombatContext,
  val engine: Engine = context.engine,
  val audioService: AudioService = context.audioService,
) : Pool.Poolable, GameEventListener {
  lateinit var entity: Entity

  open fun onAdd() = Unit

  abstract fun isFinished(): Boolean

  open fun onRemove() = Unit

  companion object {
    val LOG = logger<Buff>()
  }
}
