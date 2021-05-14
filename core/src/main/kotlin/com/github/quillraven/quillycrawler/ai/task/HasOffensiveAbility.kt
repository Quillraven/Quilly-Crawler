package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.StatsType
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.ashley.component.statsCmp
import com.github.quillraven.quillycrawler.combat.command.CommandAiType

class HasOffensiveAbility : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    val currentMana = `object`.statsCmp[StatsType.MANA]
    `object`.combatCmp.availableCommands.values().forEach {
      if (it.aiType == CommandAiType.OFFENSIVE && it.manaCost > 0 && currentMana >= it.manaCost) {
        return Status.SUCCEEDED
      }
    }

    return Status.FAILED
  }
}
