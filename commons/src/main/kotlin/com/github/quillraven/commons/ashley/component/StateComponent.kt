package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.StateSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Interface for a [State] of an [Entity]. Defines default empty implementations for
 * all [State] functions.
 */
interface EntityState : State<Entity> {
    override fun enter(entity: Entity) = Unit

    override fun update(entity: Entity) = Unit

    override fun exit(entity: Entity) = Unit

    override fun onMessage(entity: Entity, telegram: Telegram) = false

    companion object {
        val EMPTY_STATE = object : EntityState {}
    }
}

/**
 * Component to store AI state related data of an [Entity]. It is used for the [StateSystem].
 * Uses a [DefaultStateMachine] to handle states.
 *
 * Use [state] to switch to a new state the next time [StateSystem.update] is called.
 *
 * Refer to [stateTime] to know how long an entity is already inside a specific state.
 *
 * Use [stateCmp] to easily access the [StateComponent] of an [Entity]. Only use it if you are sure that
 * the component is not null. Otherwise, it will throw a [GdxRuntimeException].
 */
class StateComponent : Component, Pool.Poolable {
    var state = EntityState.EMPTY_STATE
    var stateTime = 0f
        internal set
    // Keep stateMachine internal to avoid calling changeState at any time during a frame.
    // That way we can guarantee that AI is always processed within the StateSystem.
    internal val stateMachine = DefaultStateMachine<Entity, State<Entity>>()

    override fun reset() {
        stateTime = 0f
        state = EntityState.EMPTY_STATE
        stateMachine.globalState = null
        stateMachine.owner = null
    }

    companion object {
        val MAPPER = mapperFor<StateComponent>()
    }
}

/**
 * Returns a [StateComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.stateCmp: StateComponent
    get() = this[StateComponent.MAPPER]
        ?: throw GdxRuntimeException("StateComponent for entity '$this' is null")
