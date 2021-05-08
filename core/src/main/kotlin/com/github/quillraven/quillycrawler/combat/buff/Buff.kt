package com.github.quillraven.quillycrawler.combat.buff

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.event.GameEventListener
import ktx.log.logger

abstract class Buff(
  private val engine: Engine,
  val audioService: AudioService
) : Pool.Poolable, GameEventListener {
  lateinit var target: Entity

  open fun onAdd() = Unit

  abstract fun isFinished(): Boolean

  open fun onRemove() = Unit

  companion object {
    val LOG = logger<Buff>()
  }
}
