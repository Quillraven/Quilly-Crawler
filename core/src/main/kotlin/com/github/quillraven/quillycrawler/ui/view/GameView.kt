package com.github.quillraven.quillycrawler.ui.view

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.StringBuilder
import com.github.quillraven.quillycrawler.ui.SkinLabelStyle
import com.github.quillraven.quillycrawler.ui.model.GameListener
import com.github.quillraven.quillycrawler.ui.model.GameViewModel
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label

class GameView(private val viewModel: GameViewModel) :
  Table(Scene2DSkin.defaultSkin), KTable, GameListener {
  private val mapLabel: Label

  init {
    setFillParent(true)

    mapLabel = label("", SkinLabelStyle.DUNGEON_LEVEL.name) { cell ->
      cell.top().expand().padTop(7f).height(25f)
      this.alpha = 0f
    }

    // debugAll()
  }

  override fun setStage(stage: Stage?) {
    if (stage == null) {
      // active screen changes away from GameScreen
      viewModel.removeGameListener(this)
    } else {
      // GameScreen becomes active screen
      viewModel.addGameListener(this)
    }
    super.setStage(stage)
  }

  override fun onMapChange(mapName: StringBuilder) {
    with(mapLabel) {
      setText(mapName)
      clearActions()
      this += alpha(0f) then fadeIn(1f) then delay(3f) then fadeOut(2f)
    }
  }
}
