package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class CanTransform : LeafTask<Entity>() {
  private var canTransform = true

  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    return if (canTransform) {
      canTransform = false
      Status.SUCCEEDED
    } else {
      Status.FAILED
    }
  }

  override fun reset() {
    super.reset()
    canTransform = true
  }
}
