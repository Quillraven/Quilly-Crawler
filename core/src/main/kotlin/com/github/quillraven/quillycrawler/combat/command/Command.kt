package com.github.quillraven.quillycrawler.combat.command

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.component.playAnimation
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.isPlayer
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.combat.CombatContext
import com.github.quillraven.quillycrawler.event.CombatCommandStarted
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger

enum class CommandAiType {
  UNDEFINED, OFFENSIVE, DEFENSIVE, SUPPORTIVE
}

enum class CommandTargetType {
  UNDEFINED, NO_TARGET, SINGLE_TARGET, ALL_TARGETS
}

sealed class Command(
  context: CombatContext,
  val engine: Engine = context.engine,
  val audioService: AudioService = context.audioService,
  private val eventDispatcher: GameEventDispatcher = context.eventDispatcher,
) : Pool.Poolable {
  abstract val manaCost: Int
  abstract val aiType: CommandAiType
  abstract val targetType: CommandTargetType
  lateinit var entity: Entity
  var targets = GdxArray<Entity>()
  var totalTime = 0f
  private var completed = false

  open fun onStart() = Unit

  open fun onUpdate(deltaTime: Float) = Unit

  fun update(deltaTime: Float): Boolean {
    if (completed) {
      // command finished -> nothing to do
      return true
    }

    if (totalTime == 0f) {
      LOG.debug { "Executing command ${this::class.simpleName} for ${if (entity.isPlayer) "PLAYER" else "ENEMY"} entity $entity" }
      entity.statsCmp[StatsType.MANA] = entity.statsCmp[StatsType.MANA] - manaCost
      eventDispatcher.dispatchEvent<CombatCommandStarted> { this.command = this@Command }
      onStart()
    }

    totalTime += deltaTime
    onUpdate(deltaTime)
    if (isFinished()) {
      // command is finished but we will still return false for the current frame in order
      // to make sure that anything that happens in onUpdate/onFinish like dealing damage gets processed
      // before executing another command
      completed = true
      onFinish()
      entity.playAnimation("idle", 0f)
    }

    return false
  }

  open fun isFinished(): Boolean = totalTime >= 1f

  open fun onFinish() = Unit

  override fun reset() {
    completed = false
    totalTime = 0f
    targets.clear()
  }

  fun hasSufficientMana(): Boolean = entity.statsCmp.totalStatValue(entity, StatsType.MANA) >= manaCost

  companion object {
    private val LOG = logger<Command>()
  }
}

abstract class CommandUnknown(context: CombatContext) : Command(context)
