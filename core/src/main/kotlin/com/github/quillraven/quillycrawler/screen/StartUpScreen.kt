package com.github.quillraven.quillycrawler.screen

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.*
import com.github.quillraven.quillycrawler.ui.configureSkin
import com.github.quillraven.quillycrawler.ui.model.StartUpViewModel
import com.github.quillraven.quillycrawler.ui.view.StartUpView
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.graphics.use
import ktx.log.debug
import ktx.log.logger
import kotlin.system.measureTimeMillis

class StartUpScreen(private val game: QuillyCrawler) : AbstractScreen(game) {
  private lateinit var viewModel: StartUpViewModel
  private val background = Texture("graphics/title_splash.png")

  override fun show() {
    // load mandatory UI assets
    val timeForUI = measureTimeMillis {
      I18NBundle.setExceptionOnMissingKey(false)
      val bundle = assetStorage.loadSync(I18NAssets.DEFAULT.descriptor)
      configureSkin(assetStorage)
      viewModel = StartUpViewModel(bundle, game)
      stage.addActor(StartUpView(viewModel))
    }
    LOG.debug { "Took '$timeForUI' ms to load UI assets" }

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
        // special debug setting in game.properties -> go to render debug screen directly without main menu
        game.addScreen(DebugRenderScreen(game))
        game.setScreen<DebugRenderScreen>()
      }

      audioService.play(MusicAssets.MOUNTAINS)
    }
  }

  override fun hide() {
    game.preferences.flush()
    game.removeScreen<StartUpScreen>()
    super.hide()
    dispose()
  }

  override fun render(delta: Float) {
    game.uiViewport.apply()
    game.batch.setColor(1f, 1f, 1f, 0.7f)
    game.batch.use(game.uiViewport.camera) {
      it.draw(background, 0f, 0f)
    }
    game.batch.setColor(1f, 1f, 1f, 1f)
    super.render(delta)
  }

  override fun dispose() {
    super.dispose()
    background.dispose()
  }

  companion object {
    private val LOG = logger<StartUpScreen>()
  }
}
