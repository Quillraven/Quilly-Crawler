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
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger
import kotlin.reflect.KClass

enum class CommandAiType {
  UNDEFINED, OFFENSIVE, DEFENSIVE, SUPPORTIVE
}

enum class CommandTargetType {
  UNDEFINED, NO_TARGET, SINGLE_TARGET
}

class CommandRequest : Pool.Poolable {
  lateinit var type: KClass<out Command>
  val targets: GdxArray<Entity> = GdxArray()

  override fun reset() {
    targets.clear()
  }
}

sealed class Command(
  context: CombatContext,
  val engine: Engine = context.engine,
  val audioService: AudioService = context.audioService
) : Pool.Poolable {
  abstract val manaCost: Int
  abstract val aiType: CommandAiType
  abstract val targetType: CommandTargetType
  lateinit var entity: Entity
  var targets = GdxArray<Entity>()
  var totalTime = 0f

  open fun onStart() = Unit

  open fun onUpdate(deltaTime: Float) = Unit

  fun update(deltaTime: Float): Boolean {
    if (totalTime == 0f) {
      LOG.debug { "Executing command ${this::class.simpleName} for ${if (entity.isPlayer) "PLAYER" else "ENEMY"} entity $entity" }
      onStart()
    }

    totalTime += deltaTime
    onUpdate(deltaTime)
    return if (isFinished()) {
      entity.statsCmp[StatsType.MANA] = entity.statsCmp[StatsType.MANA] - manaCost
      onFinish()
      entity.playAnimation("idle", 0f)
      true
    } else {
      false
    }
  }

  abstract fun isFinished(): Boolean

  open fun onFinish() = Unit

  override fun reset() {
    totalTime = 0f
    targets.clear()
  }

  companion object {
    private val LOG = logger<Command>()
  }
}
