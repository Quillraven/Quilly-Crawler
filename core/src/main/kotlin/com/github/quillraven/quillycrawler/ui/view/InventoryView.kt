package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.InventoryViewModel
import ktx.scene2d.KTable
import ktx.scene2d.table
import ktx.scene2d.textButton

class InventoryView(skin: Skin, private val viewModel: InventoryViewModel) : Table(skin), KTable,
  InputProcessor, XboxInputProcessor {
  init {
    setFillParent(true)
    background = skin.getDrawable(SkinImages.WINDOW.regionKey)

    textButton("Inventory", SkinTextButtonStyle.TITLE.name) { cell ->
      this.labelCell.padTop(4f)
      cell.expand()
        .top().padTop(8f)
        .height(25f).width(95f)
        .colspan(2)
        .row()
    }

    table { cell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

      cell.expand()
        .padBottom(3f)
        .width(95f).height(115f)
    }

    table { cell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

      cell.expand()
        .padBottom(3f)
        .width(180f).height(115f)
        .row()
    }

    textButton("Press >", SkinTextButtonStyle.DEFAULT.name) { cell ->
      cell.padBottom(7f)
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

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
