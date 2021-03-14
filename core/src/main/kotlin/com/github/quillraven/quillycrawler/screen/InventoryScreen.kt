package com.github.quillraven.quillycrawler.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.ui.configureSkin
import com.github.quillraven.quillycrawler.ui.view.inventoryView

class InventoryScreen(game: AbstractGame) : AbstractScreen(game) {
  private val skin = configureSkin(game.assetStorage)
  private val stage = Stage(FitViewport(320f, 180f), batch).apply {
    addActor(inventoryView(skin))
  }

  override fun resize(width: Int, height: Int) {
    stage.viewport.update(width, height, true)
  }

  override fun render(delta: Float) {
    // TODO remove debug
    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      stage.clear()
      stage.addActor(inventoryView(skin))
    }

    stage.act(delta)
    stage.viewport.apply()
    stage.draw()
  }

  override fun dispose() {
    stage.dispose()
    skin.dispose()
  }
}
