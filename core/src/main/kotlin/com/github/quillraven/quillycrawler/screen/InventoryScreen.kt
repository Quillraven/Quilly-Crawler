package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.commons.ashley.system.RemoveSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.system.ConsumeSystem
import com.github.quillraven.quillycrawler.ashley.system.GearSystem
import com.github.quillraven.quillycrawler.ashley.system.SetScreenSystem
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.ui.model.InventoryViewModel
import com.github.quillraven.quillycrawler.ui.view.InventoryView

class InventoryScreen(game: QuillyCrawler, private val engine: Engine, playerEntity: Entity) : AbstractScreen(game) {
  val viewModel =
    InventoryViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], engine, playerEntity, game.audioService)
  private val view = InventoryView(viewModel, assetStorage[I18NAssets.DEFAULT.descriptor])
  private val stage = Stage(FitViewport(320f, 180f), batch)

  override fun resize(width: Int, height: Int) {
    stage.viewport.update(width, height, true)
  }

  override fun show() {
    engine.systems.forEach { system ->
      if (isInventoryRelevantSystem(system)) {
        system.setProcessing(false)
      }
    }

    stage.addActor(view)
  }

  override fun hide() {
    stage.clear()

    engine.systems.forEach { system ->
      if (isInventoryRelevantSystem(system)) {
        system.setProcessing(true)
      }
    }
  }

  private fun isInventoryRelevantSystem(system: EntitySystem?) =
    system !is GearSystem && system !is SetScreenSystem && system !is ConsumeSystem && system !is RemoveSystem

  override fun render(delta: Float) {
    // TODO remove debug
    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      stage.clear()
      stage.addActor(InventoryView(viewModel, assetStorage[I18NAssets.DEFAULT.descriptor]))
    }

    with(stage) {
      act(delta)
      viewport.apply()
      draw()
    }
  }

  override fun dispose() {
    stage.dispose()
  }
}
