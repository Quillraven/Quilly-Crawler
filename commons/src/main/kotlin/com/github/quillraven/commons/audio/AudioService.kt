package com.github.quillraven.commons.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * Interface for audio services to handle the playback for [Sound] and [Music].
 *
 * Refer to [QueueAudioService] for an example implementation
 */
interface AudioService {
  fun playSound(soundFilePath: String, volume: Float = 1f)

  fun playMusic(musicFilePath: String, volume: Float = 1f, loop: Boolean = true)

  fun pauseMusic()

  fun resumeMusic()

  fun stopMusic()

  fun update()
}
