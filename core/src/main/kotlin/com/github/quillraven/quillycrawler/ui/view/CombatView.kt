package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.commons.ui.widget.Bar
import com.github.quillraven.commons.ui.widget.bar
import com.github.quillraven.quillycrawler.ui.*
import com.github.quillraven.quillycrawler.ui.model.CombatUiListener
import com.github.quillraven.quillycrawler.ui.model.CombatViewModel
import ktx.actors.plusAssign
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
  private val orderButtons = GdxArray<TextButton>()
  private var activeOrderIdx = IDX_ATTACK
  private val lifeBar: Bar
  private val manaBar: Bar

  init {
    setFillParent(true)

    //
    // Turn Label
    //
    turnLabel = label(turnLabelText, SkinLabelStyle.FRAMED_BRIGHT.name) { cell ->
      cell.left().top()
        .expand().height(17f)
        .padTop(3f).padLeft(3f)
        .colspan(2)
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
      defaults().left().padBottom(2f)

      textButton(
        "$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.attack"]}[]",
        SkinTextButtonStyle.BRIGHT.name
      ) { it.row() }
      textButton(
        "$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.ability"]}[]",
        SkinTextButtonStyle.BRIGHT.name
      ) { it.row() }
      textButton("$TXT_COLOR_BLACK${this@CombatView.bundle["CombatView.item"]}[]", SkinTextButtonStyle.BRIGHT.name)
      children.forEach { btn -> this@CombatView.orderButtons.add(btn as TextButton) }

      cell.bottom().left().padLeft(3f).padBottom(1f).bottom()
    }

    //
    // Ability / Item selection
    //
    table { cell ->
      background = skin.getDrawable(SkinImages.BUTTON_2.regionKey)

      cell.expand().left().bottom()
        .padBottom(3f)
        .padLeft(15f)
        .width(80f).height(50f)

      isVisible = true
    }

    //
    // Life / Mana bars of player
    table { cell ->
      label("10 / 10", SkinLabelStyle.LARGE.name) { it.padLeft(45f).height(7f).row() }
      this@CombatView.lifeBar = bar(SkinBarStyle.LIFE.name) {
        setScale(1.5f)
        it.padBottom(3f).padLeft(40f).row()
      }

      label("10 / 10", SkinLabelStyle.LARGE.name) { it.padLeft(45f).height(7f).row() }
      this@CombatView.manaBar = bar(SkinBarStyle.MANA.name) {
        setScale(1.25f, 1.5f)
        it.padBottom(7f).padLeft(40f)
      }

      cell.expand().bottom().left().padBottom(3f)
    }

    // set scroll pane of entity order as last child to be rendered to avoid additional texture bindings
    sp.zIndex = children.size - 1

    // debugAll()
  }

  private fun selectButton(idx: Int) {
    val oldBtn = orderButtons[activeOrderIdx]
    oldBtn.clearActions()
    oldBtn += moveTo(0f, oldBtn.y, 0.25f)
    oldBtn.label.text.replace(TXT_COLOR_WHITE, TXT_COLOR_BLACK)
    oldBtn.label.invalidateHierarchy()

    activeOrderIdx = when {
      idx < 0 -> orderButtons.size - 1
      idx >= orderButtons.size -> 0
      else -> idx
    }

    val newBtn = orderButtons[activeOrderIdx]
    newBtn.clearActions()
    newBtn += moveTo(5f, newBtn.y, 0.25f)
    newBtn.label.text.replace(TXT_COLOR_BLACK, TXT_COLOR_WHITE)
    newBtn.label.invalidateHierarchy()
  }

  override fun onShow() {
    viewModel.addCombatListener(this)

    orderButtons.forEach {
      it.label.text.replace(TXT_COLOR_WHITE, TXT_COLOR_BLACK)
      it.label.invalidateHierarchy()
    }
    activeOrderIdx = IDX_ATTACK
    selectButton(activeOrderIdx)
  }

  override fun onHide() {
    viewModel.removeCombatListener(this)

    // Clear any order children since they are images and will link to a null texture which causes a crash.
    // The reason is that we pool those images to avoid recreating them and during reset the texture gets set to null.
    turnOrderTable.clear()
  }

  override fun onNextTurn(turn: Int, entityImages: GdxArray<Image>) {
    with(turnLabel.text) {
      setLength(turnLabelText.length)
      append(" ").append(turn)
      turnLabel.invalidateHierarchy()
    }

    turnOrderTable.clear()
    for (i in 0 until entityImages.size) {
      turnOrderTable.add(entityImages[entityImages.size - 1 - i].apply { layout() }).height(16f).row()
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.ESCAPE -> viewModel.returnToGame()
      Input.Keys.A -> {
        viewModel.selectTarget()
        viewModel.orderAttack()
      }
      Input.Keys.UP -> selectButton(activeOrderIdx - 1)
      Input.Keys.DOWN -> selectButton(activeOrderIdx + 1)
      Input.Keys.NUM_1 -> lifeBar.fill(0f)
      Input.Keys.NUM_2 -> lifeBar.fill(0.5f)
      Input.Keys.NUM_3 -> lifeBar.fill(1f)
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

  companion object {
    private val IDX_ATTACK = 0
    private val IDX_ABILITY = 1
    private val IDX_ITEM = 2
    private const val TXT_COLOR_WHITE = "[#ffffff]"
    private const val TXT_COLOR_BLACK = "[#000000]"
  }
}
