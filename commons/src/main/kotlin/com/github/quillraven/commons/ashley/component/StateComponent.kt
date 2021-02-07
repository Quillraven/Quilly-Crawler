package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.badlogic.gdx.ai.fsm.State
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.ashley.get
import ktx.ashley.mapperFor

interface IState : State<Entity> {
    override fun enter(entity: Entity?) = Unit

    override fun update(entity: Entity?) = Unit

    override fun exit(entity: Entity?) = Unit

    override fun onMessage(entity: Entity?, telegram: Telegram?) = false

    companion object {
        val EMPTY_STATE = object : IState {}
    }
}

class StateComponent : Component, Pool.Poolable {
    var stateTime = 0f
    var state = IState.EMPTY_STATE
    val stateMachine = DefaultStateMachine<Entity, State<Entity>>()

    override fun reset() {
        stateTime = 0f
        state = IState.EMPTY_STATE
        stateMachine.globalState = null
        stateMachine.owner = null
    }

    companion object {
        val MAPPER = mapperFor<StateComponent>()
    }
}

val Entity.state: StateComponent
    get() = this[StateComponent.MAPPER]
        ?: throw GdxRuntimeException("StateComponent for entity '$this' is null")
