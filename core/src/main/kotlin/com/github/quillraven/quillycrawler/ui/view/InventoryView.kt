package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinListStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.InventoryViewModel
import ktx.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.List as GdxList

class InventoryView(private val viewModel: InventoryViewModel) : Table(Scene2DSkin.defaultSkin), KTable,
  InputProcessor, XboxInputProcessor {
  private val bagItems: GdxList<String>
  private val itemImage: Image
  private val itemDescription: Label

  init {
    setFillParent(true)
    background = skin.getDrawable(SkinImages.WINDOW.regionKey)

    // header
    textButton("Inventory", SkinTextButtonStyle.TITLE.name) { cell ->
      this.labelCell.padTop(4f)
      cell.expand()
        .top().padTop(8f)
        .height(25f).width(95f)
        .colspan(2)
        .row()
    }

    // item bag table
    table { cell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)
      defaults().expand().fill()

      this@InventoryView.bagItems =
        listWidget<String>(SkinListStyle.DEFAULT.name).cell(padTop = 3f, padLeft = 2f, padRight = 2f)

      cell.expand()
        .padBottom(3f)
        .width(95f).height(115f)
    }

    // item details and stats table
    table { tableCell ->
      background = skin.getDrawable(SkinImages.FRAME_1.regionKey)

      this@InventoryView.itemImage = image(skin.getDrawable(SkinImages.UNDEFINED.regionKey)) { cell ->
        cell.top().padLeft(4f).padTop(6f)
      }
      this@InventoryView.itemDescription = label("", SkinLabelStyle.DEFAULT.name) { cell ->
        setAlignment(Align.left)
        wrap = true
        cell.expandX().fill()
          .padLeft(4f).padRight(4f).padTop(6f)
          .row()
      }

      // TODO add separator and stats information
      table { dummyCell -> dummyCell.expand().fill().colspan(2) }

      tableCell.expand()
        .padBottom(3f)
        .width(180f).height(115f)
        .row()
    }

    // controls table
    table { cell ->
      defaults().padRight(2f)

      image(skin.getDrawable(SkinImages.GAME_PAD_DOWN.regionKey))
      image(skin.getDrawable(SkinImages.GAME_PAD_UP.regionKey))
      image(skin.getDrawable(SkinImages.KEY_BOARD_DOWN.regionKey))
      image(skin.getDrawable(SkinImages.KEY_BOARD_UP.regionKey))
      label("scroll through bag", SkinLabelStyle.DEFAULT.name) { it.padTop(4f) }

      image(skin.getDrawable(SkinImages.GAME_PAD_A.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_SPACE.regionKey))
      label("equip or use item", SkinLabelStyle.DEFAULT.name) { it.padTop(4f) }

      image(skin.getDrawable(SkinImages.GAME_PAD_B.regionKey)) { it.padLeft(20f) }
      image(skin.getDrawable(SkinImages.KEY_BOARD_ESCAPE.regionKey))
      label("exit", SkinLabelStyle.DEFAULT.name) { it.padTop(4f) }

      cell.expand().left()
        .colspan(2)
        .padLeft(12f).padBottom(6f)
    }

    // debugAll()
  }

  override fun setStage(stage: Stage?) {
    if (stage == null) {
      // active screen changes away from InventoryScreen
      removeXboxControllerListener()
      Gdx.input.inputProcessor = null
    } else {
      // InventoryScreen becomes active screen
      addXboxControllerListener()
      Gdx.input.inputProcessor = this

      bagItems.run {
        clearItems()
        viewModel.load { items, firstItemIndex, regionKey, description ->
          setItems(items)
          onSelectionChange(firstItemIndex, regionKey, description)
        }
      }
    }
    super.setStage(stage)
  }

  private fun onSelectionChange(newIndex: Int, regionKey: String, description: String) {
    bagItems.selectedIndex = newIndex

    itemImage.drawable = skin.getDrawable(regionKey)
    itemImage.isVisible = regionKey != SkinImages.UNDEFINED.regionKey

    itemDescription.setText(description)
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.DOWN -> viewModel.moveItemSelectionIndex(1, ::onSelectionChange)
      Input.Keys.UP -> viewModel.moveItemSelectionIndex(-1, ::onSelectionChange)
      Input.Keys.SPACE -> viewModel.equipOrUseSelectedItem()
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

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_DOWN -> viewModel.moveItemSelectionIndex(1, ::onSelectionChange)
      XboxInputProcessor.BUTTON_UP -> viewModel.moveItemSelectionIndex(-1, ::onSelectionChange)
      XboxInputProcessor.BUTTON_A -> viewModel.equipOrUseSelectedItem()
      XboxInputProcessor.BUTTON_B -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }

  override fun buttonUp(controller: Controller?, buttonCode: Int) = false

  override fun axisMoved(controller: Controller?, axisCode: Int, value: Float) = false
}
