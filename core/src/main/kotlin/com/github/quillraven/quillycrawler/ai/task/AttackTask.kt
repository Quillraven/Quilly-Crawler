package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.quillycrawler.combat.CombatBlackboard
import ktx.log.debug
import ktx.log.logger

class AttackTask : LeafTask<CombatBlackboard>() {
  private var previousStateKey = ""
  private var previousAnimationSpeed = 0f
  private var previousPlayMode = Animation.PlayMode.NORMAL

  override fun copyTo(task: Task<CombatBlackboard>) = task

  override fun start() {
    `object`.owner.animationCmp.run {
      previousStateKey = stateKey
      previousAnimationSpeed = animationSpeed
      previousPlayMode = playMode
      stateKey = "run"
      animationSpeed = 1f
      playMode = Animation.PlayMode.NORMAL
    }
  }

  override fun end() {
    `object`.owner.animationCmp.run {
      stateKey = previousStateKey
      animationSpeed = previousAnimationSpeed
      playMode = previousPlayMode
    }
  }

  override fun execute(): Status {
    return if (`object`.owner.animationCmp.isAnimationFinished()) {
      LOG.debug { "Attacking like crazy" }
      Status.SUCCEEDED
    } else {
      Status.RUNNING
    }
  }

  companion object {
    private val LOG = logger<AttackTask>()
  }
}
