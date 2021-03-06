package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.StateMachine
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.collections.getOrPut

/**
 * System to set and update an [entity's][Entity] [EntityState]. It also updates
 * the [messageManager] and the [timepiece][GdxAI.getTimepiece].
 *
 * Automatically adds and removes the [StateComponent.stateMachine] as a listener for all
 * given [messageTypes] whenever an entity with a [StateComponent] gets added or removed
 * from the [Engine].
 *
 * If the [StateComponent.state] is equal to [EntityState.EMPTY_STATE] then the system
 * will update the [StateComponent.stateMachine] and [StateComponent.stateTime].
 * Otherwise, it will call the state machine's [changeState][StateMachine.changeState] function
 * and reset the [StateComponent.stateTime] to 0.
 *
 * Additionally, if an entity has an [AnimationComponent] then this system is setting its [AnimationComponent.stateKey].
 * In order for that to work you need to stick to a certain naming convention. The **regionKey** of the
 * [TextureAtlas] must be "regionKey/%state%" where %state% is the lowercase name of the [EntityState].
 * E.g. if your regionKey is "wizard" and your state is called "IDLE" then the region in the atlas
 * must be called "wizard/idle".
 */
class StateSystem(
  private val messageManager: MessageManager,
  private val messageTypes: Set<Int> = setOf()
) : IteratingSystem(allOf(StateComponent::class).exclude(RemoveComponent::class).get()), EntityListener {
  private val stateAnimationStringCache = ObjectMap<EntityState, String>()

  /**
   * Adds the system as an [EntityListener] for the [family]
   */
  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    engine.addEntityListener(family, this)
  }

  /**
   * Removes the system as an [EntityListener]
   */
  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
  }

  /**
   * Sets the [state machine's][StateComponent.stateMachine] [owner][DefaultStateMachine.owner]
   * and adds the machine as a listener for all [messageTypes].
   */
  override fun entityAdded(entity: Entity) {
    with(entity.stateCmp) {
      stateMachine.owner = entity

      for (messageType in messageTypes) {
        messageManager.addListener(stateMachine, messageType)
      }
    }
  }

  /**
   * Removes the [state machine][StateComponent.stateMachine] as a listener for all [messageTypes].
   */
  override fun entityRemoved(entity: Entity) {
    with(entity.stateCmp) {
      for (messageType in messageTypes) {
        messageManager.removeListener(stateMachine, messageType)
      }
    }
  }

  /**
   * Updates the [time piece][GdxAI.getTimepiece], [messageManager] and all entities.
   */
  override fun update(deltaTime: Float) {
    GdxAI.getTimepiece().update(deltaTime)
    messageManager.update()
    super.update(deltaTime)
  }

  /**
   * Updates the entity's [StateComponent] by either switching to a new [EntityState]
   * or by updating the current [EntityState].
   */
  override fun processEntity(entity: Entity, deltaTime: Float) {
    with(entity.stateCmp) {
      when {
        EntityState.EMPTY_STATE != state -> {
          // switch to new state
          stateTime = 0f
          stateMachine.changeState(state)
          entity[AnimationComponent.MAPPER]?.let {
            it.stateKey = stateAnimationStringCache.getOrPut(state) { state.toString().toLowerCase() }
          }
          state = EntityState.EMPTY_STATE
        }
        else -> {
          // update current state
          stateTime += deltaTime
          stateMachine.update()
        }
      }
    }
  }
}
