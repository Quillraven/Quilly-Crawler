package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.quillraven.commons.input.XboxInputProcessor
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin

abstract class View(private val inputView: Boolean = true) : Table(Scene2DSkin.defaultSkin), KTable,
  InputProcessor, XboxInputProcessor {
  private var initialized = false

  override fun setStage(stage: Stage?) {
    if (stage == null) {
      initialized = false
      if (inputView) {
        removeInputControl()
      }
      onHide()
    } else {
      if (inputView) {
        setInputControl()
      }
    }
    super.setStage(stage)
  }

  fun setInputControl() {
    addXboxControllerListener()
    Gdx.input.inputProcessor = this
  }

  fun removeInputControl() {
    removeXboxControllerListener()
    Gdx.input.inputProcessor = null
  }

  override fun draw(batch: Batch?, parentAlpha: Float) {
    super.draw(batch, parentAlpha)
    if (!initialized) {
      // onShow is called here AFTER draw is called because actors' position gets modified within draw
      // and for certain initialization logic we already need the correct position of actors.
      // It is weird to me that a call to pack doesn't solve that but I don't want to waste more time on that
      // and this is an acceptable hack-around ;)
      initialized = true
      onShow()
    }
  }

  abstract fun onShow()

  abstract fun onHide()

  override fun keyDown(keycode: Int) = false

  override fun keyUp(keycode: Int) = false

  override fun keyTyped(character: Char) = false

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false

  override fun mouseMoved(screenX: Int, screenY: Int) = false

  override fun scrolled(amountX: Float, amountY: Float) = false

  override fun buttonDown(controller: Controller?, buttonCode: Int) = false

  override fun buttonUp(controller: Controller?, buttonCode: Int) = false

  override fun axisMoved(controller: Controller?, axisCode: Int, value: Float) = false
}
