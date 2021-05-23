package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinScrollPaneStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.CombatUiListener
import com.github.quillraven.quillycrawler.ui.model.CombatViewModel
import ktx.collections.GdxArray
import ktx.scene2d.label
import ktx.scene2d.scrollPane
import ktx.scene2d.table
import ktx.scene2d.textButton

class CombatView(
  private val viewModel: CombatViewModel,
  val bundle: I18NBundle = viewModel.bundle
) : View(), CombatUiListener {
  private val turnLabel: Label
  private val turnOrderTable: Table
  private val turnLabelText = bundle["CombatView.turn"]

  init {
    setFillParent(true)

    //
    // Turn Label
    //
    turnLabel = label(turnLabelText, SkinLabelStyle.FRAMED_BRIGHT.name) { cell ->
      cell.left().top()
        .expand().height(17f)
        .padTop(2.5f).padLeft(2.5f)
    }

    //
    // Table that shows the order of entities for the next round
    //
    // !!! Make sure that this is the last child to be rendered because it is using
    // !!! the normal entity atlas instead of the UI atlas to render the entity's textures.
    // !!! If it is not the last child then there are additional texture bindings
    // !!! refer to the call below 'sp.zIndex = children.size - 1
    turnOrderTable = table {
      top()
    }
    val sp = scrollPane(SkinScrollPaneStyle.DEFAULT.name) { cell ->
      setScrollingDisabled(true, false)
      setScrollbarsVisible(false)

      actor = this@CombatView.turnOrderTable

      cell.top().right()
        .width(35f).height(85f)
        .padRight(3f).padTop(3f)
        .row()
    }

    //
    // Command Buttons for attack, abilities and items
    //
    table { cell ->
      defaults().left().expand().padBottom(2f)

      textButton(this@CombatView.bundle["CombatView.attack"], SkinTextButtonStyle.BRIGHT.name) { it.row() }
      textButton(this@CombatView.bundle["CombatView.ability"], SkinTextButtonStyle.BRIGHT.name) { it.row() }
      textButton(this@CombatView.bundle["CombatView.item"], SkinTextButtonStyle.BRIGHT.name)

      cell.padLeft(3f).padBottom(1f).fill()
    }

    // set scroll pane of entity order as last child to be rendered to avoid additional texture bindings
    sp.zIndex = children.size - 1

    // debugAll()
  }

  override fun onShow() {
    viewModel.addCombatListener(this)
  }

  override fun onHide() {
    viewModel.removeCombatListener(this)
    turnOrderTable.clear()
  }

  override fun onNextTurn(turn: Int, entityImages: GdxArray<Image>) {
    with(turnLabel.text) {
      setLength(turnLabelText.length)
      append(" ").append(turn)
    }

    turnOrderTable.clear()
    for (i in 0 until entityImages.size) {
      turnOrderTable.add(entityImages[entityImages.size - 1 - i].apply { layout() }).height(16f).row()
    }

    invalidateHierarchy()
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      Input.Keys.A -> {
        viewModel.selectTarget()
        viewModel.orderAttack()
      }
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_B -> viewModel.returnToGame()
      else -> return false
    }

    return true
  }
}
