package com.github.quillraven.quillycrawler.screen

import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.assets.MusicAssets
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import com.github.quillraven.quillycrawler.ui.configureSkin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.logger
import kotlin.system.measureTimeMillis

class StartUpScreen(private val game: QuillyCrawler) : AbstractScreen(game) {
  override fun show() {
    // load mandatory UI assets
    val timeForUI = measureTimeMillis {
      assetStorage.loadSync(I18NAssets.DEFAULT.descriptor)
      configureSkin(assetStorage)
    }
    LOG.debug { "Took '$timeForUI' ms to load UI assets" }

    // TODO setup UI for screen to show loading progress, splash-art, etc.

    // load remaining assets in the background
    val timeForAssets = System.currentTimeMillis()
    val assets = gdxArrayOf(
      TextureAtlasAssets.values().filter { it != TextureAtlasAssets.UI }.map { assetStorage.loadAsync(it.descriptor) },
      SoundAssets.values().map { assetStorage.loadAsync(it.descriptor) },
      MusicAssets.values().map { assetStorage.loadAsync(it.descriptor) }
    ).flatten()
    KtxAsync.launch {
      assets.joinAll()
      LOG.debug { "Took '${(System.currentTimeMillis() - timeForAssets)}' ms to load remaining assets" }

      if (game.renderDebug()) {
        game.addScreen(DebugRenderScreen(game))
        game.setScreen<DebugRenderScreen>()
      } else {
        game.addScreen(GameScreen(game))
        game.setScreen<GameScreen>()
      }
    }
  }

  companion object {
    private val LOG = logger<StartUpScreen>()
  }
}
