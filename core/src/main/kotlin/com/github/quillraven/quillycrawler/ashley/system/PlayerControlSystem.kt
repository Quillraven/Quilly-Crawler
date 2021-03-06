package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageManager
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.ashley.component.StateComponent
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ai.MessageType
import com.github.quillraven.quillycrawler.ashley.component.InteractComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.ashley.component.moveCmp
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class PlayerControlSystem(
  private val messageManager: MessageManager
) : InputProcessor, XboxInputProcessor,
  IteratingSystem(allOf(PlayerControlComponent::class).exclude(RemoveComponent::class).get()) {
  private var valueLeftX = 0f
  private var valueLeftY = 0f
  private var stopMovement = true
  private var moveDirectionDeg = 0f
  private var actionPressed = false

  override fun addedToEngine(engine: Engine?) {
    super.addedToEngine(engine)
    Gdx.input.inputProcessor = this
    addXboxControllerListener()
  }

  override fun removedFromEngine(engine: Engine?) {
    super.removedFromEngine(engine)
    Gdx.input.inputProcessor = null
    removeXboxControllerListener()
  }

  override fun setProcessing(processing: Boolean) {
    super.setProcessing(processing)
    if (processing) {
      Gdx.input.inputProcessor = this
      addXboxControllerListener()
    } else {
      Gdx.input.inputProcessor = null
      removeXboxControllerListener()
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.D -> {
        updateMovementValues(1f, valueLeftY)
        return true
      }
      Input.Keys.A -> {
        updateMovementValues(-1f, valueLeftY)
        return true
      }
      Input.Keys.W -> {
        updateMovementValues(valueLeftX, -1f)
        return true
      }
      Input.Keys.S -> {
        updateMovementValues(valueLeftX, 1f)
        return true
      }
      Input.Keys.SPACE -> {
        actionPressed = true
        return true
      }
    }
    return false
  }

  override fun keyUp(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.D -> {
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
          updateMovementValues(-1f, valueLeftY)
        } else {
          updateMovementValues(0f, valueLeftY)
        }
        return true
      }
      Input.Keys.A -> {
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
          updateMovementValues(1f, valueLeftY)
        } else {
          updateMovementValues(0f, valueLeftY)
        }
        return true
      }
      Input.Keys.W -> {
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
          updateMovementValues(valueLeftX, 1f)
        } else {
          updateMovementValues(valueLeftX, 0f)
        }
        return true
      }
      Input.Keys.S -> {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
          updateMovementValues(valueLeftX, -1f)
        } else {
          updateMovementValues(valueLeftX, 0f)
        }

        return true
      }
      Input.Keys.SPACE -> {
        actionPressed = false
        return true
      }
    }
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
        updateMovementValues(value, valueLeftY)
        return true
      }
      XboxInputProcessor.AXIS_Y_LEFT -> {
        updateMovementValues(valueLeftX, value)
        return true
      }
    }

    return false
  }

  private fun updateMovementValues(newX: Float, newY: Float) {
    valueLeftX = newX
    valueLeftY = newY
    stopMovement = abs(valueLeftX) <= AXIS_DEAD_ZONE && abs(valueLeftY) <= AXIS_DEAD_ZONE
    moveDirectionDeg = atan2(-valueLeftY, valueLeftX) * 180f / PI.toFloat()
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    updateMovement(entity)
    processAction(entity)
  }

  private fun updateMovement(entity: Entity) {
    with(entity.moveCmp) {
      root = stopMovement
      if (!root) {
        cosDeg = MathUtils.cosDeg(moveDirectionDeg)
        sinDeg = MathUtils.sinDeg(moveDirectionDeg)
      }
    }
  }

  private fun processAction(entity: Entity) {
    if (!actionPressed) {
      return
    }

    entity[InteractComponent.MAPPER]?.let { interactCmp ->
      interactCmp.entitiesInRange.forEach { actionableEntity ->
        actionableEntity[StateComponent.MAPPER]?.let {
          // dispatch message to update entity state
          messageManager.dispatchMessage(MessageType.PLAYER_INTERACT_ENTITY.ordinal)
        }
      }
    }
  }

  companion object {
    private const val AXIS_DEAD_ZONE = 0.25f
  }
}
