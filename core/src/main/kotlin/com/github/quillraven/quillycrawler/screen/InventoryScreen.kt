package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.game.AbstractGame
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.ashley.system.GearSystem
import com.github.quillraven.quillycrawler.ashley.system.SetScreenSystem
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.ui.configureSkin
import com.github.quillraven.quillycrawler.ui.model.InventoryViewModel
import com.github.quillraven.quillycrawler.ui.view.InventoryView

class InventoryScreen(game: AbstractGame, private val engine: Engine, playerEntity: Entity) : AbstractScreen(game) {
  private val skin = configureSkin(game.assetStorage)
  private val viewModel = InventoryViewModel(game.assetStorage[I18NAssets.DEFAULT.descriptor], engine, playerEntity)
  private val view = InventoryView(skin, viewModel)
  private val stage = Stage(FitViewport(320f, 180f), batch).apply {
    addActor(view)
  }

  override fun resize(width: Int, height: Int) {
    stage.viewport.update(width, height, true)
  }

  override fun show() {
    engine.systems.forEach { system ->
      if (system !is GearSystem && system !is SetScreenSystem) {
        system.setProcessing(false)
      }
    }
    view.addXboxControllerListener()
    Gdx.input.inputProcessor = view

    viewModel.initialize()
    view.initialize()
  }

  override fun hide() {
    view.removeXboxControllerListener()
    Gdx.input.inputProcessor = null
    engine.systems.forEach { system ->
      if (system !is GearSystem && system !is SetScreenSystem) {
        system.setProcessing(true)
      }
    }
  }

  override fun render(delta: Float) {
    // TODO remove debug
    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      stage.clear()
      viewModel.initialize()
      stage.addActor(InventoryView(skin, viewModel).apply {
        initialize()
      })
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
