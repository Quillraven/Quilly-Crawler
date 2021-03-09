package com.github.quillraven.quillycrawler.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.commons.ashley.component.EntityState
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.stateCmp
import com.github.quillraven.quillycrawler.ashley.component.ActionableComponent

enum class ChestState : EntityState {
  IDLE {
    override fun onMessage(entity: Entity, telegram: Telegram): Boolean {
      if (telegram.message == MessageType.PLAYER_INTERACT.ordinal) {
        entity.stateCmp.state = OPEN
        return true
      }
      return false
    }
  },

  OPEN {
    override fun enter(entity: Entity) {
      entity.animationCmp.playMode = Animation.PlayMode.NORMAL
      // remove ActionableComponent to avoid that a player can open a chest multiple times
      entity.remove(ActionableComponent::class.java)
    }
  }
}
