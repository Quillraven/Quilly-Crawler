package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ashley.component.InteractComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.ashley.component.SetScreenComponent
import com.github.quillraven.quillycrawler.ashley.component.moveCmp
import com.github.quillraven.quillycrawler.event.GameEventDispatcher
import com.github.quillraven.quillycrawler.event.GameExitEvent
import com.github.quillraven.quillycrawler.screen.InventoryScreen
import ktx.ashley.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.reflect.KClass

class PlayerControlSystem(private val eventDispatcher: GameEventDispatcher) : InputProcessor, XboxInputProcessor,
  IteratingSystem(allOf(PlayerControlComponent::class).exclude(RemoveComponent::class).get()) {
  private var valueLeftX = 0f
  private var valueLeftY = 0f
  private var stopMovement = true
  private var moveDirectionDeg = 0f
  private var actionPressed = false
  private var nextScreen: KClass<out AbstractScreen> = AbstractScreen::class

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
    if (processing && Gdx.input.inputProcessor != this) {
      Gdx.input.inputProcessor = this
      addXboxControllerListener()
    } else if (!processing && Gdx.input.inputProcessor == this) {
      Gdx.input.inputProcessor = null
      removeXboxControllerListener()
    }
    super.setProcessing(processing)
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.RIGHT -> updateMovementValues(1f, valueLeftY)
      Input.Keys.LEFT -> updateMovementValues(-1f, valueLeftY)
      Input.Keys.UP -> updateMovementValues(valueLeftX, -1f)
      Input.Keys.DOWN -> updateMovementValues(valueLeftX, 1f)
      Input.Keys.SPACE -> actionPressed = true
      Input.Keys.I -> {
        updateMovementValues(0f, 0f)
        nextScreen = InventoryScreen::class
      }
      Input.Keys.ESCAPE -> eventDispatcher.dispatchEvent<GameExitEvent>()
      else -> return false
    }

    return true
  }

  override fun keyUp(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.RIGHT -> {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
          updateMovementValues(-1f, valueLeftY)
        } else {
          updateMovementValues(0f, valueLeftY)
        }
      }
      Input.Keys.LEFT -> {
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
          updateMovementValues(1f, valueLeftY)
        } else {
          updateMovementValues(0f, valueLeftY)
        }
      }
      Input.Keys.UP -> {
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
          updateMovementValues(valueLeftX, 1f)
        } else {
          updateMovementValues(valueLeftX, 0f)
        }
      }
      Input.Keys.DOWN -> {
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
          updateMovementValues(valueLeftX, -1f)
        } else {
          updateMovementValues(valueLeftX, 0f)
        }
      }
      Input.Keys.SPACE -> actionPressed = false
      else -> return false
    }

    return true
  }

  override fun keyTyped(character: Char) = false

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false

  override fun mouseMoved(screenX: Int, screenY: Int) = false

  override fun scrolled(amountX: Float, amountY: Float) = false

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_A -> actionPressed = true
      XboxInputProcessor.BUTTON_Y -> nextScreen = InventoryScreen::class
      XboxInputProcessor.BUTTON_START -> eventDispatcher.dispatchEvent<GameExitEvent>()
      else -> return false
    }

    return true
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

    if (nextScreen != AbstractScreen::class) {
      engine.configureEntity(entity) { with<SetScreenComponent> { screenType = nextScreen } }
      nextScreen = AbstractScreen::class
    }
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

    actionPressed = false
    entity[InteractComponent.MAPPER]?.let { it.interact = true }
  }

  companion object {
    private const val AXIS_DEAD_ZONE = 0.25f
  }
}
