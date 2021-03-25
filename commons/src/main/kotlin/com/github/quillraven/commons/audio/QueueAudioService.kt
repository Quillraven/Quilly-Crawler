package com.github.quillraven.commons.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import kotlinx.coroutines.launch
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.getOrPut
import ktx.collections.iterate
import ktx.collections.set
import ktx.log.debug
import ktx.log.info
import ktx.log.logger
import kotlin.math.max

private class SoundRequest : Pool.Poolable {
  var filePath: String = ""
  var volume = 1f

  override fun reset() {
    filePath = ""
    volume = 1f
  }
}

/**
 * Implementation of [AudioService] to play one [Music] track at a time and to queue [Sound] playback
 * to avoid multiple [Sound] instances of the same sound within a single frame.
 *
 * Requires an [assetStorage] to load and unload the [Sound] and [Music] resources.
 *
 * Use [maxSimultaneousSounds] to define the maximum number of sounds that can play concurrently within one frame.
 *
 * Use [maxCachedSounds] to define the size of the sound cache. [Sound] instances are cached internally for
 * faster access of future calls. If the [maxCachedSounds] is reached then the cache gets cleared.
 */
class QueueAudioService(
  private val assetStorage: AssetStorage,
  private val maxSimultaneousSounds: Int = 16,
  private val maxCachedSounds: Int = 50
) : AudioService {

  private val soundRequests = ObjectMap<String, SoundRequest>(maxSimultaneousSounds)
  private val soundCache = ObjectMap<String, Sound>(maxCachedSounds)
  private var currentMusicFilePath: String = ""
  private var currentMusic: Music? = null

  /**
   * Adds a [SoundRequest] to the request queue for the [soundFilePath] and [volume].
   * If the [maxSimultaneousSounds] is reached for the current frame then this request is ignored.
   * Making a call to [update] will play all pending requests.
   */
  override fun playSound(soundFilePath: String, volume: Float) {
    if (soundRequests.size >= maxSimultaneousSounds) {
      LOG.info { "Reached maximum of '$maxSimultaneousSounds' simultaneous sounds. Request is ignored" }
      return
    }

    val request = soundRequests[soundFilePath]
    if (request != null) {
      // sound already queued -> set volume to maximum of both requests
      request.volume = max(request.volume, volume).coerceIn(0f, 1f)
    } else {
      // sound not queued yet
      soundRequests[soundFilePath] = SOUND_REQUEST_POOL.obtain().apply {
        this.volume = volume
        this.filePath = soundFilePath
      }
    }
  }

  /**
   * Loads and plays a new [Music] instance for [musicFilePath] with the given [volume].
   * If [loop] is true then the music will start again when finished.
   *
   * If there is already a music playing then it gets stopped and unloaded from the [assetStorage].
   */
  override fun playMusic(musicFilePath: String, volume: Float, loop: Boolean) {
    if (currentMusicFilePath == musicFilePath) {
      // trying to play the same music again -> ignore it
      LOG.debug { "Music '$musicFilePath' is already playing" }
      return
    }

    val oldMusic = currentMusic
    if (oldMusic != null) {
      // stop and unload current music
      LOG.debug { "Stop music '$currentMusicFilePath'" }
      oldMusic.stop()
      KtxAsync.launch {
        assetStorage.unload<Music>(currentMusicFilePath)
      }
    }

    // load and play new music
    LOG.debug { "Play music '$musicFilePath'" }
    currentMusicFilePath = musicFilePath
    currentMusic = assetStorage.loadSync<Music>(musicFilePath).apply {
      this.isLooping = loop
      this.volume = volume.coerceIn(0f, 1f)
      play()
    }
  }

  /**
   * Pause the current active [Music].
   */
  override fun pauseMusic() {
    currentMusic?.pause()
  }

  /**
   * Resume the current active [Music].
   */
  override fun resumeMusic() {
    currentMusic?.play()
  }

  /**
   * Stop the current active [Music].
   */
  override fun stopMusic() {
    currentMusic?.stop()
  }

  /**
   * Processes all queued [sound requests][SoundRequest] of [playSound] and clears the queue.
   * Will load the [Sound] via the [assetStorage] if needed and adds the [Sound] to an internal
   * cache for faster access in the future.
   *
   * If the cache reaches the [maxCachedSounds] size then the [Sound] instances of the cache get unloaded
   * from the [assetStorage] and the cache gets cleared.
   */
  override fun update(deltaTime: Float) {
    if (!soundRequests.isEmpty) {
      soundRequests.iterate { _, request, iterator ->
        val sound = soundCache.getOrPut(request.filePath) {
          // sound not loaded yet -> load it
          if (soundCache.size >= maxCachedSounds) {
            // maximum cache size reached -> unload sounds and clear cache
            LOG.info { "Clearing sound cache of size '$maxCachedSounds'" }
            soundCache.iterate { soundFilePath, _, cacheIterator ->
              KtxAsync.launch {
                assetStorage.unload<Sound>(soundFilePath)
              }
              cacheIterator.remove()
            }
          }

          // load sound
          assetStorage.loadSync(request.filePath)
        }

        // play sound and remove request
        LOG.debug { "Play sound '${request.filePath}'" }
        sound.play(request.volume)
        SOUND_REQUEST_POOL.free(request)
        iterator.remove()
      }
    }
  }

  companion object {
    private val LOG = logger<QueueAudioService>()
    private val SOUND_REQUEST_POOL = object : Pool<SoundRequest>() {
      override fun newObject() = SoundRequest()
    }
  }
}
