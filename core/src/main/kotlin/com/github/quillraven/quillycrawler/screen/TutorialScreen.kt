package com.github.quillraven.quillycrawler.screen

import com.github.quillraven.commons.game.AbstractScreen
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.I18NAssets
import com.github.quillraven.quillycrawler.ui.model.TutorialViewModel
import com.github.quillraven.quillycrawler.ui.view.TutorialView

class TutorialScreen(game: QuillyCrawler) : AbstractScreen(game) {
  private val viewModel = TutorialViewModel(assetStorage[I18NAssets.DEFAULT.descriptor], audioService, game)
  private val view = TutorialView(viewModel)

  override fun show() {
    super.show()
    stage.addActor(view)
  }
}
