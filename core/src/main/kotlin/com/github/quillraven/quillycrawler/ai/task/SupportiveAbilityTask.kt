package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.combatAICmp
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.ashley.component.randomSupportiveCommand
import ktx.log.error
import ktx.log.logger

class SupportiveAbilityTask : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    val cmd = `object`.randomSupportiveCommand()

    if (cmd == null) {
      LOG.error { "Called SupportiveAbilityTask for AI entity without supportive abilities or with insufficient mana" }
      return Status.FAILED
    }

    `object`.combatAICmp.setCommandTargets(cmd)
    `object`.combatCmp.newCommand(cmd)
    return Status.SUCCEEDED
  }

  companion object {
    private val LOG = logger<SupportiveAbilityTask>()
  }
}
