package com.github.quillraven.quillycrawler.ui.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.I18NBundle
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.QuillyCrawler
import com.github.quillraven.quillycrawler.QuillyCrawler.Companion.PREF_KEY_MUSIC
import com.github.quillraven.quillycrawler.QuillyCrawler.Companion.PREF_KEY_SOUND
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.screen.GameScreen
import ktx.preferences.set

class StartUpViewModel(
  val bundle: I18NBundle,
  private val game: QuillyCrawler,
  private val audioService: AudioService = game.audioService,
  private val preferences: Preferences = game.preferences
) {
  private var musicVolume: Float = preferences.getFloat(PREF_KEY_MUSIC, 1f)
  private var soundVolume: Float = preferences.getFloat(PREF_KEY_SOUND, 1f)

  fun changeOption() {
    audioService.play(SoundAssets.MENU_SELECT)
  }

  fun musicVolume() = audioService.musicVolume

  fun soundVolume() = audioService.soundVolume

  fun hasGameState(): Boolean = false

  fun changeMusicVolume(amount: Float): Float {
    audioService.play(SoundAssets.MENU_SELECT_2)
    musicVolume = MathUtils.clamp(musicVolume + amount, 0f, 1f)
    audioService.musicVolume = musicVolume
    preferences[PREF_KEY_MUSIC] = audioService.musicVolume
    return musicVolume
  }

  fun changeSoundVolume(amount: Float): Float {
    soundVolume = MathUtils.clamp(soundVolume + amount, 0f, 1f)
    audioService.soundVolume = soundVolume
    audioService.play(SoundAssets.MENU_SELECT_2)
    preferences[PREF_KEY_SOUND] = audioService.soundVolume
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
