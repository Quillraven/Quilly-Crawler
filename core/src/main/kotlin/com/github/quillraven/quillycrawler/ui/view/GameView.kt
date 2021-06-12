package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.model.GameUiListener
import com.github.quillraven.quillycrawler.ui.model.GameViewModel
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.scene2d.label

class GameView(private val viewModel: GameViewModel) : View(false), GameUiListener {
  private val mapLabel: Label

  init {
    setFillParent(true)

    mapLabel = label("", SkinLabelStyle.DUNGEON_LEVEL.name) { cell ->
      cell.top().expand().padTop(7f).height(25f)
      this.alpha = 0f
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
}
