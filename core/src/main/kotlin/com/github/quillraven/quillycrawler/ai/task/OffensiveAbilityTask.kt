package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.combatAICmp
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.combat.command.Command
import com.github.quillraven.quillycrawler.combat.command.CommandAiType
import ktx.collections.GdxArray
import ktx.log.error
import ktx.log.logger

class OffensiveAbilityTask : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    TMP_ARRAY.clear()
    val combatCmp = `object`.combatCmp
    val currentMana = `object`.statsCmp[StatsType.MANA]
    combatCmp.availableCommands.values().forEach {
      if (it.aiType == CommandAiType.OFFENSIVE && it.manaCost > 0 && currentMana >= it.manaCost) {
        TMP_ARRAY.add(it)
      }
    }

    if (TMP_ARRAY.isEmpty) {
      LOG.error { "Called offensiveAbility for an AI entity without an offensive ability or with insufficient mana" }
      return Status.FAILED
    }

    val command = TMP_ARRAY.random()
    command.run {
      targets.clear()
      targets.add(`object`.combatAICmp.randomPlayerEntity())
    }
    combatCmp.newCommand(command)

    return Status.SUCCEEDED
  }

  companion object {
    private val TMP_ARRAY = GdxArray<Command>()
    private val LOG = logger<OffensiveAbilityTask>()
  }
}
