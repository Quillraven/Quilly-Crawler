package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.ashley.component.state
import ktx.ashley.allOf

class StateSystem : IteratingSystem(allOf(StateComponent::class).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val state = entity.state

        if (state.stateMachine.owner == null) {
            // initialize logic of newly created component
            state.stateMachine.owner = entity
            state.stateMachine.changeState(state.state)
        } else if (state.stateMachine.currentState != state.state) {
            // switch to next state
            state.stateTime = 0f
            state.stateMachine.changeState(state.state)
        } else {
            // update current state
            state.stateTime += deltaTime
            state.stateMachine.update()
        }
    }
}