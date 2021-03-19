package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.github.quillraven.commons.ashley.component.RemoveComponent
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.ashley.component.SetScreenComponent
import com.github.quillraven.quillycrawler.ashley.component.setScreenCmp
import com.github.quillraven.quillycrawler.screen.InventoryScreen
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

class SetScreenSystem(private val game: QuillyCrawler) :
  IteratingSystem(allOf(SetScreenComponent::class).exclude(RemoveComponent::class).get()) {
  override fun processEntity(entity: Entity, deltaTime: Float) {
    val nextScreenType = entity.setScreenCmp.screenType

    if (nextScreenType == AbstractScreen::class) {
      LOG.error { "Cannot set AbstractScreen as a screen" }
      return
    }

    if (!game.containsScreen(nextScreenType.java)) {
      // create screen if not yet created
      LOG.debug { "Screen '${nextScreenType.simpleName}' does not exist yet -> create it" }
      when (nextScreenType) {
        InventoryScreen::class -> game.addScreen(InventoryScreen(game, engine, entity))
        else -> {
          LOG.error { "Unsupported screen type '${nextScreenType.simpleName}'" }
          return
        }
      }
    } else {
      // screen already exists -> update parameters if necessary
      if (nextScreenType == InventoryScreen::class) {
        game.getScreen<InventoryScreen>().viewModel.playerEntity = entity
      }
    }

    // change to next screen
    game.setScreen(nextScreenType.java)

    entity.remove(SetScreenComponent::class.java)
  }

  companion object {
    private val LOG = logger<SetScreenSystem>()
  }
}
