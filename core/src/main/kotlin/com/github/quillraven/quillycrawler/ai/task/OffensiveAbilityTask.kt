package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.combatAICmp
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.ashley.component.randomOffensiveCommand
import ktx.log.error
import ktx.log.logger

class OffensiveAbilityTask : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    val cmd = `object`.randomOffensiveCommand()

    if (cmd == null) {
      LOG.error { "Called OffensiveAbilityTask for AI entity without offensive abilities or with insufficient mana" }
      return Status.FAILED
    }

    `object`.combatAICmp.setCommandTargets(cmd)
    `object`.combatCmp.newCommand(cmd)
    return Status.SUCCEEDED
  }

  companion object {
    private val LOG = logger<OffensiveAbilityTask>()
  }
}
