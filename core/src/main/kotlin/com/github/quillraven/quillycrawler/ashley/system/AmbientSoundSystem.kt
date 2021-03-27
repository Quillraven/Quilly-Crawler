package com.github.quillraven.quillycrawler.ashley.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.math.MathUtils
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.quillycrawler.assets.SoundAssets
import com.github.quillraven.quillycrawler.assets.play
import com.github.quillraven.quillycrawler.assets.stop
import ktx.collections.gdxArrayOf

class AmbientSoundSystem(
  private val audioService: AudioService
) : EntitySystem() {
  private var nextSfx = timeForNextSfx()
  private val sfxArray = gdxArrayOf(SoundAssets.DRAGON_GROWL_00, SoundAssets.DRAGON_GROWL_01, SoundAssets.GOBLIN_03)

  private fun timeForNextSfx() = MathUtils.random(30f, 55f)

  override fun addedToEngine(engine: Engine) {
    super.addedToEngine(engine)
    audioService.play(SoundAssets.AMBIENCE_CAVE, true)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    audioService.stop(SoundAssets.AMBIENCE_CAVE)
  }

  override fun setProcessing(processing: Boolean) {
    super.setProcessing(processing)
    if (processing) {
      audioService.play(SoundAssets.AMBIENCE_CAVE, true)
    } else if (!processing) {
      audioService.stop(SoundAssets.AMBIENCE_CAVE)
    }
  }

  override fun update(deltaTime: Float) {
    nextSfx -= deltaTime
    if (nextSfx <= 0f) {
      nextSfx = timeForNextSfx()
      audioService.play(sfxArray.random())
    }
  }
}
