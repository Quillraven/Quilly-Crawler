package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.combat.CombatBlackboard
import ktx.log.debug
import ktx.log.logger
import kotlin.math.max

class DefendTask : LeafTask<CombatBlackboard>() {
  private var defendTime = 0f

  override fun copyTo(task: Task<CombatBlackboard>) = task

  override fun start() {
    defendTime = 2f
    LOG.debug { "DEFENDING" }
  }

  override fun execute(): Status {
    defendTime = max(0f, defendTime - Gdx.graphics.deltaTime)
    if (defendTime <= 0f) {
      return Status.SUCCEEDED
    } else {
      return Status.RUNNING
    }
  }

  companion object {
    private val LOG = logger<DefendTask>()
  }
}
