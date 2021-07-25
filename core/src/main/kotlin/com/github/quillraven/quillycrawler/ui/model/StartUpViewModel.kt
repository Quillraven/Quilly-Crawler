package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.screen.GameScreen

class StartUpViewModel(
  val bundle: I18NBundle,
  private val game: QuillyCrawler,
  private val audioService: AudioService = game.audioService
) {
  private var musicVolume: Float = 1f // preferences.getFloat("music-volume", 1f)
  private var soundVolume: Float = 1f //preferences.getFloat("sound-volume", 1f)

  fun changeOption() {
    audioService.play(SoundAssets.MENU_SELECT)
  }

  fun hasGameState(): Boolean = false

  fun changeMusicVolume(amount: Float): Float {
    audioService.play(SoundAssets.MENU_SELECT_2)
    musicVolume = MathUtils.clamp(musicVolume + amount, 0f, 1f)
    audioService.musicVolume = musicVolume
    return musicVolume
  }

  fun changeSoundVolume(amount: Float): Float {
    soundVolume = MathUtils.clamp(soundVolume + amount, 0f, 1f)
    audioService.soundVolume = soundVolume
    audioService.play(SoundAssets.MENU_SELECT_2)
    return soundVolume
  }

  fun newGame() {
    audioService.play(SoundAssets.MENU_SELECT)
    if (!game.containsScreen<GameScreen>()) {
      game.addScreen(GameScreen(game))
    }
    game.setScreen<GameScreen>()
  }

  fun continueGame() {
    audioService.play(SoundAssets.MENU_SELECT)
    // TODO
    newGame()
  }

  fun quitGame() {
    audioService.play(SoundAssets.MENU_SELECT)
    Gdx.app.exit()
  }
}
