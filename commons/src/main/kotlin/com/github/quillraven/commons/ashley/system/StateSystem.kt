package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.ashley.component.stateCmp
import ktx.ashley.allOf

class StateSystem : IteratingSystem(allOf(StateComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val stateCmp = entity.stateCmp

        when {
            stateCmp.stateMachine.owner == null -> {
                // initialize logic of newly created component
                stateCmp.stateMachine.owner = entity
                stateCmp.stateMachine.changeState(stateCmp.state)
            }
            stateCmp.stateMachine.currentState != stateCmp.state -> {
                // switch to next state
                stateCmp.stateTime = 0f
                stateCmp.stateMachine.changeState(stateCmp.state)
            }
            else -> {
                // update current state
                stateCmp.stateTime += deltaTime
                stateCmp.stateMachine.update()
            }
        }
    }
}