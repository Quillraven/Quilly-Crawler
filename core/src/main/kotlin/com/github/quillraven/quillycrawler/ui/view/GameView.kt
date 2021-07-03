package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.GameUiListener
import com.github.quillraven.quillycrawler.ui.model.GameViewModel
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.isNotEmpty
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton

class GameView(private val viewModel: GameViewModel, bundle: I18NBundle = viewModel.bundle) : View(false),
  GameUiListener {
  private val mapLabel: Label
  private val resetTable: Table
  private val resetLabel: Label
  private val btnYes: TextButton
  private val btnNo: TextButton

  init {
    setFillParent(true)

    mapLabel = label("", SkinLabelStyle.DUNGEON_LEVEL.name) { cell ->
      cell.top().padTop(7f).height(25f).row()
      this.alpha = 0f
    }

    resetTable = table { tableCell ->
      background = skin.getDrawable(SkinImages.WINDOW.regionKey)

      this@GameView.resetLabel = label("", SkinLabelStyle.LARGE.name) { cell ->
        cell.width(126f).padTop(6f).padBottom(10f).colspan(2).row()
        this.wrap = true
      }

      this@GameView.btnYes = textButton(bundle["YES"], SkinTextButtonStyle.BRIGHT.name) { cell ->
        cell.pad(0f, 7f, 6f, 0f).left().expandX()
        this.label.color = Color.BLACK
      }
      this@GameView.btnNo = textButton(bundle["NO"], SkinTextButtonStyle.BRIGHT.name) { cell ->
        cell.pad(0f, 0f, 6f, 7f).right().expandX()
        this.label.color = Color.BLACK
      }

      tableCell.width(140f).top().expand().padTop(10f)
      isVisible = false
    }

    // debugAll()
  }

  override fun onShow() {
    // GameScreen becomes active screen
    viewModel.addGameListener(this)
  }

  override fun onHide() {
    // active screen changes away from GameScreen
    viewModel.removeGameListener(this)
  }

  override fun onMapChange(mapName: StringBuilder) {
    with(mapLabel) {
      setText(mapName)
      clearActions()
      this += alpha(0f) then fadeIn(1f) then delay(3f) then fadeOut(2f)
    }
  }

  override fun onDungeonReset(goldLoss: Int, newLevel: Int) {
    setInputControl()

    resetTable.isVisible = true
    resetLabel.setText("Do you want to go back to a previous dungeon?\nYou will be reset to level $newLevel and lose $goldLoss gold.")
    btnNo.label.addSelectionEffect()
  }

  private fun changeYesNo() {
    if (btnYes.label.actions.isNotEmpty()) {
      // yes button currently selected -> switch to no
      btnYes.label.removeSelectionEffect()
      btnNo.label.addSelectionEffect()
    } else {
      // no btn selected -> switch to yes
      btnNo.label.removeSelectionEffect()
      btnYes.label.addSelectionEffect()
    }
    viewModel.switchSelection()
  }

  private fun confirmDungeonReset(resetDungeon: Boolean) {
    removeInputControl()
    btnNo.label.removeSelectionEffect()
    btnYes.label.removeSelectionEffect()
    resetTable.isVisible = false
    viewModel.resetDungeon(resetDungeon)
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.LEFT, Input.Keys.RIGHT -> changeYesNo()
      Input.Keys.SPACE -> confirmDungeonReset(btnYes.label.actions.isNotEmpty())
      Input.Keys.ESCAPE -> confirmDungeonReset(false)
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_LEFT, XboxInputProcessor.BUTTON_RIGHT -> changeYesNo()
      XboxInputProcessor.BUTTON_A -> confirmDungeonReset(btnYes.label.actions.isNotEmpty())
      XboxInputProcessor.BUTTON_B -> confirmDungeonReset(false)
      else -> return false
    }

    return true
  }
}
