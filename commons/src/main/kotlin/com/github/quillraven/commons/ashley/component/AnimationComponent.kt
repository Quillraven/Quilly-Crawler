package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

interface IAnimationType {
    val atlasAsset: ITextureAtlasAssets
    val atlasKey: String
    val speed: Float
    val playMode: Animation.PlayMode
}

class AnimationComponent : Component, Pool.Poolable {
    var dirty = true
        private set
    var stateTime = 0f
    var type = EMPTY_ANIMATION_TYPE
        set(value) {
            dirty = value != field
            field = value
        }
    var gdxAnimation: Animation<TextureRegion> = EMPTY_ANIMATION
        set(value) {
            dirty = false
            stateTime = 0f
            field = value
        }

    override fun reset() {
        dirty = true
        stateTime = 0f
        type = EMPTY_ANIMATION_TYPE
    }

    companion object {
        val MAPPER = mapperFor<AnimationComponent>()
        val EMPTY_ANIMATION = Animation<TextureRegion>(0f, gdxArrayOf())
        val EMPTY_ANIMATION_TYPE = object : IAnimationType {
            override val atlasAsset = object : ITextureAtlasAssets {
                override val descriptor = AssetDescriptor("", TextureAtlas::class.java)
            }
            override val atlasKey = ""
            override val speed = 0f
            override val playMode = Animation.PlayMode.LOOP
        }
    }
}

val Entity.animation: AnimationComponent
    get() = this[AnimationComponent.MAPPER]
        ?: throw GdxRuntimeException("AnimationComponent for entity '$this' is null")
