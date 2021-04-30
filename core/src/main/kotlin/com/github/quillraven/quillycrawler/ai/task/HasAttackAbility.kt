package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.combat.CombatBlackboard

class HasAttackAbility : LeafTask<CombatBlackboard>() {
  override fun copyTo(task: Task<CombatBlackboard>) = task

  override fun execute(): Status {
    return Status.SUCCEEDED
  }
}
