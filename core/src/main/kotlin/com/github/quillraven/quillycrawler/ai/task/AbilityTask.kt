package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.combat.CombatBlackboard
import ktx.log.debug
import ktx.log.logger

class AbilityTask : LeafTask<CombatBlackboard>() {

  override fun copyTo(task: Task<CombatBlackboard>) = task

  override fun execute(): Status {
    LOG.debug { "Spellcast!" }
    return Status.SUCCEEDED
  }

  companion object {
    private val LOG = logger<AbilityTask>()
  }
}
