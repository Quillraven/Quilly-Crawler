package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.controllers.Controller
import com.github.quillraven.commons.ashley.component.MoveComponent
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import ktx.ashley.allOf
import ktx.ashley.get
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class PlayerControlSystem(
    private val messageManager: MessageManager
) : InputProcessor, XboxInputProcessor,
    IteratingSystem(allOf(PlayerControlComponent::class).get()) {
    private var valueLeftX = 0f
    private var valueLeftY = 0f
    private var stopMovement = true
    private var moveDirectionDeg = 0f
    private var actionPressed = false

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
        if (buttonCode == XboxInputProcessor.BUTTON_A) {
            actionPressed = true
            return true
        }
        return false
    }

    override fun buttonUp(controller: Controller?, buttonCode: Int): Boolean {
        if (buttonCode == XboxInputProcessor.BUTTON_A) {
            actionPressed = false
            return true
        }
        return false
    }

    override fun axisMoved(controller: Controller?, axisCode: Int, value: Float): Boolean {
        when (axisCode) {
            XboxInputProcessor.AXIS_X_LEFT -> {
                valueLeftX = value
                stopMovement = abs(valueLeftX) <= 0.1f && abs(valueLeftY) <= 0.1f
                moveDirectionDeg = atan2(-valueLeftY, valueLeftX) * 180f / PI.toFloat()
                return true
            }
            XboxInputProcessor.AXIS_Y_LEFT -> {
                valueLeftY = value
                stopMovement = abs(valueLeftX) <= 0.1f && abs(valueLeftY) <= 0.1f
                moveDirectionDeg = atan2(-valueLeftY, valueLeftX) * 180f / PI.toFloat()
                return true
            }
        }

        return false
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        updateMovement(entity)
        processAction(entity)
    }

    private fun updateMovement(entity: Entity) {
        val moveCmp = entity[MoveComponent.MAPPER]
        if (moveCmp == null && !stopMovement) {
            entity.add(engine.createComponent(MoveComponent::class.java).apply {
                directionDeg = moveDirectionDeg
                speed = 2.5f
            })
        } else if (moveCmp != null) {
            if (stopMovement) {
                entity.remove(MoveComponent::class.java)
            } else {
                moveCmp.directionDeg = moveDirectionDeg
            }
        }
    }

    private fun processAction(entity: Entity) {
        if (!actionPressed) {
            return
        }

        entity[CollectingComponent.MAPPER]?.let { collectingCmp ->
            collectingCmp.entitiesInRange.forEach { collectableEntity ->
                collectableEntity[StateComponent.MAPPER]?.let {
                    // dispatch message to update entity state
                    messageManager.dispatchMessage(MessageType.PLAYER_COLLECT_ENTITY.ordinal)
                }
            }
        }
    }
}