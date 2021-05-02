package com.github.quillraven.commons.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import ktx.assets.async.AssetStorage
import ktx.collections.GdxSet
import ktx.collections.iterate
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger
import kotlin.math.max

/**
 * A request for a [Sound] that is created within the [QueueAudioService.playSound] method.
 * It contains the [filePath] and the [volume] of the request.
 * Requests are getting processed each time the [QueueAudioService.update] method is called which should be
 * done per frame.
 */
private class SoundRequest : Pool.Poolable {
  var filePath: String = ""
  var volume = 1f
  var loop = false

  override fun reset() {
    filePath = ""
    volume = 1f
    loop = false
  }
}

/**
 * Implementation of [AudioService] to play one [Music] track at a time and to queue [Sound] playback
 * to avoid multiple [Sound] instances of the same sound within a single frame.
 *
 * Requires an [assetStorage] to access the [Sound] and [Music] resources.
 *
 * Use [maxSimultaneousSounds] to define the maximum number of sounds that can play concurrently within one frame.
 *
 * Use [musicVolume] and [soundVolume] to set the volume of [Music] and [Sound] instances. The range is between 0 and 1.
 */
class QueueAudioService(
  private val assetStorage: AssetStorage,
  private val maxSimultaneousSounds: Int = 16
) : AudioService {
  // cap volume between 0f and 1f
  override var musicVolume: Float = 1f
    set(value) {
      field = value.coerceIn(0f, 1f)
      currentMusic?.volume = field
    }
  override var soundVolume: Float = 1f
    set(value) {
      field = value.coerceIn(0f, 1f)
    }

  private val soundRequests = ObjectMap<String, SoundRequest>(maxSimultaneousSounds)
  private var currentMusicFilePath: String = ""
  private var previousMusicPath = ""
  private var currentMusic: Music? = null
  private val playedSounds = GdxSet<String>(maxSimultaneousSounds)

  /**
   * Adds a [SoundRequest] to the request queue for the [soundFilePath] and [volume].
   * If [loop] is true then the sound will start again when finished.
   * If the [maxSimultaneousSounds] is reached for the current frame then this request is ignored.
   * If the [Sound] is not loaded yet by the [assetStorage] then the request is also ignored.
   * Making a call to [update] will play all pending requests.
   */
  override fun playSound(soundFilePath: String, volume: Float, loop: Boolean) {
    // verify sound is loaded
    if (!assetStorage.isLoaded<Sound>(soundFilePath)) {
      LOG.error { "Sound '$soundFilePath' is not loaded. Request is ignored!" }
      return
    }

    // verify that we did not reach the maximum amount of simultaneous sounds
    if (soundRequests.size >= maxSimultaneousSounds) {
      LOG.debug { "Reached maximum of '$maxSimultaneousSounds' simultaneous sounds. Request is ignored" }
      return
    }

    // add or update request
    playedSounds.add(soundFilePath)
    val request = soundRequests[soundFilePath]
    if (request != null) {
      // sound already queued -> set volume to maximum of both requests
      request.volume = max(request.volume, volume)
    } else {
      // sound not queued yet
      soundRequests[soundFilePath] = SOUND_REQUEST_POOL.obtain().apply {
        this.volume = volume
        this.filePath = soundFilePath
        this.loop = loop
      }
    }
  }

  /**
   * Plays [Music] from [musicFilePath] with the given [volume].
   * If [loop] is true then the music will start again when finished.
   * If there is already a music playing then it gets stopped.
   * If the [Music] is not loaded yet by the [assetStorage] then the call is ignored.
   */
  override fun playMusic(musicFilePath: String, volume: Float, loop: Boolean) {
    // verify that music is loaded
    if (!assetStorage.isLoaded<Music>(musicFilePath)) {
      LOG.error { "Music '$musicFilePath' is not loaded. Request is ignored!" }
      return
    }

    if (currentMusicFilePath == musicFilePath) {
      // trying to play the same music again -> ignore it
      LOG.debug { "Music '$musicFilePath' is already playing" }
      return
    }

    // stop previous music if there is any
    currentMusic?.stop()

    // play new music
    LOG.debug { "Play music '$musicFilePath'" }
    previousMusicPath = currentMusicFilePath
    currentMusicFilePath = musicFilePath
    currentMusic = assetStorage.get<Music>(musicFilePath).apply {
      this.isLooping = loop
      this.volume = (volume * musicVolume).coerceIn(0f, 1f)
      play()
    }
  }

  /**
   * Returns the playback of the previous [Music] which was set via [playMusic].
   * If there was no call to [playMusic] yet then this function does nothing.
   */
  override fun playPreviousMusic(volume: Float, loop: Boolean) {
    if (previousMusicPath.isNotBlank()) {
      playMusic(previousMusicPath)
    }
  }

  /**
   * Pauses the current active [Music] and all previously started [Sound] instances.
   */
  override fun pause() {
    currentMusic?.pause()
    playedSounds.forEach {
      assetStorage.get<Sound>(it).pause()
    }
  }

  /**
   * Resumes the current active [Music] and all previously paused [Sound] instances.
   */
  override fun resume() {
    currentMusic?.play()
    playedSounds.forEach {
      assetStorage.get<Sound>(it).resume()
    }
  }

  /**
   * Stop the current active [Music].
   */
  override fun stopMusic() {
    currentMusic?.stop()
  }

  /**
   * Stops all [Sound] instances of the given [soundFilePath].
   */
  override fun stopSounds(soundFilePath: String) {
    // verify sound is loaded
    if (!assetStorage.isLoaded<Sound>(soundFilePath)) {
      LOG.error { "Sound '$soundFilePath' is not loaded and cannot be stopped!" }
      return
    }

    assetStorage.get<Sound>(soundFilePath).stop()
  }

  /**
   * Processes all queued [sound requests][SoundRequest] of [playSound] and clears the queue. This method
   * should be called each frame.
   */
  override fun update() {
    if (!soundRequests.isEmpty) {
      soundRequests.iterate { _, request, iterator ->
        // play sound and remove request
        LOG.debug { "Play sound '${request.filePath}'" }
        if (request.loop) {
          assetStorage.get<Sound>(request.filePath).loop((request.volume * soundVolume).coerceIn(0f, 1f))
        } else {
          assetStorage.get<Sound>(request.filePath).play((request.volume * soundVolume).coerceIn(0f, 1f))
        }
        SOUND_REQUEST_POOL.free(request)
        iterator.remove()
      }
    }
  }

  companion object {
    private val LOG = logger<QueueAudioService>()
    private val SOUND_REQUEST_POOL = object : Pool<SoundRequest>(16) {
      override fun newObject() = SoundRequest()
    }
  }
}
