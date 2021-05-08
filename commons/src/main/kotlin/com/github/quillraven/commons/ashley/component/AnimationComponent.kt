package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.AnimationSystem
import com.github.quillraven.commons.ashley.system.StateSystem
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

/**
 * Component to store [Animation] related data. It is used for the [AnimationSystem].
 *
 * Use [atlasFilePath] and [regionKey] to define which [regions][TextureRegion] to use for the animation.
 * Whenever you change the value of one of those fields then the [AnimationSystem] will set the new animation.
 *
 * Use [stateKey] if you want to link an [EntityState] to an animation. Have a look at the [AnimationSystem]
 * and [StateSystem] as an example.
 *
 * Use [playMode] and [animationSpeed] to define how and how fast the animation is played.
 *
 * Use [stateTime] to define the current frame of the animation. 0 is the first frame of the animation.
 * This gets updated automatically by the [AnimationSystem].
 *
 * Use [animationCmp] to easily access the [AnimationComponent] of an [Entity]. Only use it if you are sure that
 * the component is not null. Otherwise, it will throw a [GdxRuntimeException].
 *
 * Use [isAnimationFinished] to check if an animation is at the last frame if played in NORMAL mode.
 */
class AnimationComponent : Component, Pool.Poolable {
  var atlasFilePath = ""
    set(value) {
      dirty = value != field
      field = value
    }
  var regionKey = ""
    set(value) {
      dirty = value != field
      field = value
    }
  var stateKey = ""
    set(value) {
      stateTime = 0f
      dirty = value != field
      field = value
    }
  var playMode = Animation.PlayMode.LOOP
  var animationSpeed = 1f
  var stateTime = 0f
  internal var gdxAnimation: Animation<TextureRegion> = EMPTY_ANIMATION
    set(value) {
      dirty = false
      stateTime = 0f
      field = value
    }
  internal var dirty = true
    private set

  override fun reset() {
    atlasFilePath = ""
    regionKey = ""
    stateKey = ""
    playMode = Animation.PlayMode.LOOP
    animationSpeed = 1f
    stateTime = 0f
    dirty = true
  }

  /**
   * refer to [Animation.isAnimationFinished]
   */
  fun isAnimationFinished() = gdxAnimation.isAnimationFinished(stateTime)

  companion object {
    val MAPPER = mapperFor<AnimationComponent>()
    val EMPTY_ANIMATION = Animation<TextureRegion>(0f, gdxArrayOf())
  }
}

/**
 * Returns an [AnimationComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.animationCmp: AnimationComponent
  get() = this[AnimationComponent.MAPPER]
    ?: throw GdxRuntimeException("AnimationComponent for entity '$this' is null")
