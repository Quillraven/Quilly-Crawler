package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.input.XboxInputProcessor
import com.github.quillraven.quillycrawler.ui.SkinImages
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.SkinTextButtonStyle
import com.github.quillraven.quillycrawler.ui.model.TutorialViewModel
import ktx.actors.centerPosition
import ktx.scene2d.label
import ktx.scene2d.textButton

class TutorialView(
  private val viewModel: TutorialViewModel,
  private val bundle: I18NBundle = viewModel.bundle
) : View() {
  private val label: Label
  private val btnNext: TextButton

  init {
    background = skin.getDrawable(SkinImages.WINDOW.regionKey)

    this.label = label(viewModel.nextInfo(), SkinLabelStyle.LARGE.name) { cell ->
      cell.width(225f).height(120f).pad(4f, 4f, 0f, 2f).row()
      this.wrap = true
      this.setAlignment(Align.topLeft, Align.topLeft)
    }
    this.btnNext = textButton(bundle["NEXT"], SkinTextButtonStyle.BRIGHT.name) { cell ->
      cell.right().expandX().pad(5f, 3f, 6f, 10f).width(80f)
      this.label.color = Color.BLACK
      this.label.addSelectionEffect()
    }

    width = 240f
    height = 150f

    // debugAll()
  }

  override fun onShow() {
    centerPosition()
  }

  override fun onHide() {

  }

  private fun nextInfo() {
    if (viewModel.hasMoreInfo()) {
      label.setText(viewModel.nextInfo())
      if (!viewModel.hasMoreInfo()) {
        btnNext.setText(bundle["START-GAME"])
      }
      viewModel.playSelectionSnd()
    } else {
      viewModel.goToGame()
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Input.Keys.SPACE -> nextInfo()
      else -> return false
    }

    return true
  }

  override fun buttonDown(controller: Controller?, buttonCode: Int): Boolean {
    when (buttonCode) {
      XboxInputProcessor.BUTTON_A -> keyDown(Input.Keys.SPACE)
      else -> return false
    }

    return true
  }
}
