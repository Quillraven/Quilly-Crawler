package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.github.quillraven.commons.ashley.system.RemoveSystem
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.system.ConsumeSystem
import com.github.quillraven.quillycrawler.ashley.system.GearSystem
import com.github.quillraven.quillycrawler.ashley.system.SetScreenSystem
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.ui.model.ShopViewModel
import com.github.quillraven.quillycrawler.ui.view.ShopView

class ShopScreen(game: QuillyCrawler, private val engine: Engine, playerEntity: Entity) : AbstractScreen(game) {
  val viewModel = ShopViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], engine, playerEntity, audioService)
  private val view = ShopView(viewModel)

  private fun isInventoryRelevantSystem(system: EntitySystem) =
    system is GearSystem || system is SetScreenSystem || system is ConsumeSystem || system is RemoveSystem

  override fun show() {
    stage.addActor(view)
  }
}
