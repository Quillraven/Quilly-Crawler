package com.github.quillraven.quillycrawler.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.github.quillraven.commons.ashley.component.EntityState
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.renderCmp
import com.github.quillraven.commons.ashley.component.stateCmp
import com.github.quillraven.quillycrawler.ashley.component.moveCmp
import kotlin.math.abs

enum class PlayerState : EntityState {
  IDLE {
    override fun enter(entity: Entity) {
      with(entity.animationCmp) {
        playMode = PlayMode.NORMAL
        animationSpeed = 0f
      }
    }

    override fun update(entity: Entity) {
      val stateCmp = entity.stateCmp
      val animationCmp = entity.animationCmp

      if (!entity.moveCmp.root) {
        // player is moving -> RUN
        stateCmp.state = RUN
      } else if (stateCmp.stateTime >= 3.75f) {
        // every X seconds play the IDLE animation again
        animationCmp.animationSpeed = 1f
        if (animationCmp.isAnimationFinished()) {
          animationCmp.animationSpeed = 0f
          animationCmp.stateTime = 0f
          stateCmp.stateTime = 0f
        }
      }
    }

    override fun exit(entity: Entity) {
      with(entity.animationCmp) {
        playMode = PlayMode.LOOP
        animationSpeed = 1f
      }
    }
  },
  RUN {
    override fun update(entity: Entity) {
      val moveCmp = entity.moveCmp
      if (moveCmp.root) {
        // player movement stopped -> IDLE
        entity.stateCmp.state = IDLE
      } else {
        // flip player sprite depending on the move direction
        with(entity.renderCmp) {
          if (abs(moveCmp.cosDeg) > 0.01f) {
            sprite.setFlip(moveCmp.cosDeg < 0, sprite.isFlipY)
          }
        }
      }
    }
  }
}
