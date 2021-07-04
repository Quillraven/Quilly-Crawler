package com.github.quillraven.quillycrawler.screen

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.ItemType
import com.github.quillraven.quillycrawler.ashley.component.bagCmp
import com.github.quillraven.quillycrawler.ashley.createItemEntity
import com.github.quillraven.quillycrawler.ashley.system.PlayerControlSystem
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.ui.model.ShopViewModel
import com.github.quillraven.quillycrawler.ui.view.ShopView
import ktx.ashley.getSystem
import ktx.collections.set

class ShopScreen(game: QuillyCrawler, private val engine: Engine, playerEntity: Entity, shopEntity: Entity) :
  AbstractScreen(game) {
  val viewModel =
    ShopViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], engine, playerEntity, shopEntity, audioService)
  private val view = ShopView(viewModel)

  override fun show() {
    engine.getSystem<PlayerControlSystem>().setProcessing(false)

    viewModel.setSellMode(false)
    stage.addActor(view)

    viewModel.shopEntity.bagCmp.run {
      if (items.isEmpty) {
        ItemType.values().forEach { itemType ->
          if (itemType == ItemType.UNDEFINED) {
            return@forEach
          }

          items[itemType] = engine.createItemEntity(itemType, 0)
        }
      }
    }
  }

  override fun hide() {
    super.hide()
    engine.getSystem<PlayerControlSystem>().setProcessing(true)
  }

  override fun render(delta: Float) {
    //TODO remove debug
    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      stage.clear()
      stage.addActor(ShopView(viewModel))
    }

    super.render(delta)
  }
}
