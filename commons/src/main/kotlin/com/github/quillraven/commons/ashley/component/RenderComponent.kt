package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.ashley.system.RenderSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component to store [Sprite] related data that is used for rendering in the [RenderSystem].
 * The component is initialized with an empty sprite that has no [Texture]. Make sure to
 * set a texture before calling the [RenderSystem].
 *
 * If an [Entity] has an [AnimationComponent] then the texture of the sprite gets set
 * automatically according to the current frame of the animation.
 *
 * Use [renderCmp] to easily access the [RenderComponent] of an [Entity]. Only use it if you are sure that
 * the component is not null. Otherwise, it will throw a [GdxRuntimeException].
 */
class RenderComponent : Component, Pool.Poolable {
  val sprite = Sprite()

  override fun reset() {
    sprite.texture = null
  }

  companion object {
    val MAPPER = mapperFor<RenderComponent>()
  }
}

/**
 * Returns a [RenderComponent] or throws a [GdxRuntimeException] if it doesn't exist.
 */
val Entity.renderCmp: RenderComponent
  get() = this[RenderComponent.MAPPER]
    ?: throw GdxRuntimeException("RenderComponent for entity '$this' is null")
