package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.quillycrawler.combat.command.*
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.GdxArray
import ktx.log.error
import ktx.log.logger

class CombatAIComponent : Component, Pool.Poolable {
  var treeFilePath = ""
  lateinit var behaviorTree: BehaviorTree<Entity>
  lateinit var allTargets: ImmutableArray<Entity>

  fun randomPlayerEntity(): Entity = allPlayerEntities().random()

  private fun allPlayerEntities(): GdxArray<Entity> {
    TMP_ENTITY_ARRAY.clear()

    allTargets.forEach {
      if (it.isPlayer && it.isAlive) {
        TMP_ENTITY_ARRAY.add(it)
      }
    }

    return TMP_ENTITY_ARRAY
  }

  private fun randomEnemyEntity(): Entity = allEnemyEntities().random()

  private fun allEnemyEntities(): GdxArray<Entity> {
    TMP_ENTITY_ARRAY.clear()

    allTargets.forEach {
      if (!it.isPlayer && it.isAlive) {
        TMP_ENTITY_ARRAY.add(it)
      }
    }

    return TMP_ENTITY_ARRAY
  }

  fun setCommandTargets(cmd: Command) {
    cmd.targets.clear()

    when (cmd.aiType) {
      CommandAiType.OFFENSIVE -> {
        // offensive targets:
        // single target = one random player entity
        // otherwise all player entities
        when (cmd.targetType) {
          CommandTargetType.SINGLE_TARGET -> cmd.targets.add(randomPlayerEntity())
          else -> cmd.targets.addAll(allPlayerEntities())
        }
      }
      CommandAiType.DEFENSIVE, CommandAiType.SUPPORTIVE -> {
        // defensive or supportive targets:
        // single target = random allied enemy entity
        // no target = self target
        // otherwise = all allied enemy entities
        when (cmd.targetType) {
          CommandTargetType.SINGLE_TARGET -> cmd.targets.add(randomEnemyEntity())
          CommandTargetType.NO_TARGET -> cmd.targets.add(cmd.entity)
          else -> cmd.targets.addAll(allEnemyEntities())
        }
      }
      else -> {
        LOG.error { "Cannot set targets for cmd ${cmd::class.simpleName}" }
      }
    }
  }

  override fun reset() {
    treeFilePath = ""
  }

  companion object {
    val LOG = logger<CombatAIComponent>()
    val MAPPER = mapperFor<CombatAIComponent>()
    val TMP_ENTITY_ARRAY = GdxArray<Entity>()
    val TMP_COMMAND_ARRAY = GdxArray<Command>()
  }
}

val Entity.combatAICmp: CombatAIComponent
  get() = this[CombatAIComponent.MAPPER]
    ?: throw GdxRuntimeException("CombatAIComponent for entity '$this' is null")

fun Entity.randomOffensiveCommand(): Command? {
  CombatAIComponent.TMP_COMMAND_ARRAY.clear()

  this[CombatComponent.MAPPER]?.let { combatCmp ->
    val currentMana = this.statsCmp[StatsType.MANA]
    combatCmp.availableCommands.values().forEach {
      if (it !is CommandAttack && it.aiType == CommandAiType.OFFENSIVE && currentMana >= it.manaCost) {
        CombatAIComponent.TMP_COMMAND_ARRAY.add(it)
      }
    }
  }

  return CombatAIComponent.TMP_COMMAND_ARRAY.random()
}

fun Entity.randomDefensiveCommand(): Command? {
  CombatAIComponent.TMP_COMMAND_ARRAY.clear()

  this[CombatComponent.MAPPER]?.let { combatCmp ->
    val currentMana = this.statsCmp[StatsType.MANA]
    combatCmp.availableCommands.values().forEach {
      if (it !is CommandDefend && it.aiType == CommandAiType.DEFENSIVE && currentMana >= it.manaCost) {
        CombatAIComponent.TMP_COMMAND_ARRAY.add(it)
      }
    }
  }

  return CombatAIComponent.TMP_COMMAND_ARRAY.random()
}

fun Entity.randomSupportiveCommand(): Command? {
  CombatAIComponent.TMP_COMMAND_ARRAY.clear()

  this[CombatComponent.MAPPER]?.let { combatCmp ->
    val currentMana = this.statsCmp[StatsType.MANA]
    combatCmp.availableCommands.values().forEach {
      if (it.aiType == CommandAiType.SUPPORTIVE && currentMana >= it.manaCost) {
        CombatAIComponent.TMP_COMMAND_ARRAY.add(it)
      }
    }
  }

  return CombatAIComponent.TMP_COMMAND_ARRAY.random()
}
