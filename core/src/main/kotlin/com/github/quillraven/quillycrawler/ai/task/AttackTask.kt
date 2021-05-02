package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.combatAICmp
import com.github.quillraven.quillycrawler.ashley.component.combatCmp
import com.github.quillraven.quillycrawler.combat.CombatOrderEffectAttack

class AttackTask : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    with(`object`.combatCmp) {
      effect = CombatOrderEffectAttack
      orderTargets.add(`object`.combatAICmp.randomPlayerEntity())
    }

    return Status.SUCCEEDED
  }
}
