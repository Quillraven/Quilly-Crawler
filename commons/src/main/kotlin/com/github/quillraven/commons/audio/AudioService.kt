package com.github.quillraven.commons.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * Interface for audio services to handle the playback for [Sound] and [Music].
 *
 * Refer to [QueueAudioService] for an example implementation
 */
interface AudioService {
  var musicVolume: Float
  var soundVolume: Float
  val currentMusicFilePath: String

  fun playSound(soundFilePath: String, volume: Float = 1f, loop: Boolean = false)

  fun playMusic(musicFilePath: String, volume: Float = 1f, loop: Boolean = true)

  fun playPreviousMusic(volume: Float = 1f, loop: Boolean = true)

  fun pause()

  fun resume()

  fun stopMusic()

  fun stopSounds(soundFilePath: String)

  fun update()
}

/**
 * Empty implementation of [AudioService]. Can be used as default value to avoid null services.
 */
object DefaultAudioService : AudioService {
  override var musicVolume = 1f
  override var soundVolume = 1f
  override val currentMusicFilePath = ""

  override fun playSound(soundFilePath: String, volume: Float, loop: Boolean) = Unit

  override fun playMusic(musicFilePath: String, volume: Float, loop: Boolean) = Unit

  override fun playPreviousMusic(volume: Float, loop: Boolean) = Unit

  override fun pause() = Unit

  override fun resume() = Unit

  override fun stopMusic() = Unit

  override fun stopSounds(soundFilePath: String) = Unit

  override fun update() = Unit
}
