package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.screen.GameScreen

class TutorialViewModel(
  val bundle: I18NBundle,
  private val audioService: AudioService,
  private val game: QuillyCrawler
) {
  private var currentInfo = 0
  private val maxInfo = 9

  fun nextInfo(): String {
    ++currentInfo
    return bundle["TutorialView.info${currentInfo.coerceAtMost(maxInfo)}"]
  }

  fun hasMoreInfo() = currentInfo < maxInfo

  fun playSelectionSnd() {
    audioService.play(SoundAssets.MENU_SELECT)
  }

  fun goToGame() {
    if (!game.containsScreen<GameScreen>()) {
      game.addScreen(GameScreen(game))
    }
    game.setScreen<GameScreen>()
  }
}
