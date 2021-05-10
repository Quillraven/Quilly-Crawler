package com.github.quillraven.quillycrawler.ai.task

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.github.quillraven.quillycrawler.ashley.component.addCommandRequest
import com.github.quillraven.quillycrawler.combat.command.CommandDeath

class DieTask : LeafTask<Entity>() {
  override fun copyTo(task: Task<Entity>) = task

  override fun execute(): Status {
    `object`.addCommandRequest(CommandDeath::class)

    return Status.SUCCEEDED
  }
}
