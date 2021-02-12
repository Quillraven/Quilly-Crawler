package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.github.quillraven.commons.ashley.component.MoveComponent
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import ktx.ashley.allOf
import ktx.ashley.get
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class PlayerControlSystem : InputProcessor, XboxInputProcessor,
    IteratingSystem(allOf(PlayerControlComponent::class).get()) {
    private var valueLeftX = 0f
    private var valueLeftY = 0f
    private var stopMovement = true
    private var moveDirectionDeg = 0f

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
        return false
    }

    override fun buttonUp(controller: Controller?, buttonCode: Int): Boolean {
        return false
    }

    override fun axisMoved(controller: Controller?, axisCode: Int, value: Float): Boolean {
        when (axisCode) {
            XboxInputProcessor.AXIS_X_LEFT -> {
                valueLeftX = value
                stopMovement = abs(valueLeftX) <= 0.1f && abs(valueLeftY) <= 0.1f
                moveDirectionDeg = atan2(-valueLeftY, valueLeftX) * 180f / PI.toFloat()
            }
            XboxInputProcessor.AXIS_Y_LEFT -> {
                valueLeftY = value
                stopMovement = abs(valueLeftX) <= 0.1f && abs(valueLeftY) <= 0.1f
                moveDirectionDeg = atan2(-valueLeftY, valueLeftX) * 180f / PI.toFloat()
            }
        }

        return false
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val moveCmp = entity[MoveComponent.MAPPER]
        if (moveCmp == null && !stopMovement) {
            entity.add(engine.createComponent(MoveComponent::class.java).apply {
                directionDeg = moveDirectionDeg
            })
            println("start")
        } else if (moveCmp != null) {
            if (stopMovement) {
                entity.remove(MoveComponent::class.java)
                println("stop")
            } else {
                moveCmp.directionDeg = moveDirectionDeg
                println("update")
            }
        }
    }
}