package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.gdxArrayOf

class AnimationComponent : Component, Pool.Poolable {
    var dirty = true
        private set
    var stateTime = 0f
    var animationSpeed = 1f
    var atlasAsset = ITextureAtlasAssets.EMPTY_TEXTURE_ATLAS_ASSET
        set(value) {
            dirty = value != field
            field = value
        }
    var regionKey = ""
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
        animationSpeed = 1f
        atlasAsset = ITextureAtlasAssets.EMPTY_TEXTURE_ATLAS_ASSET
        regionKey = ""
    }

    companion object {
        val MAPPER = mapperFor<AnimationComponent>()
        val EMPTY_ANIMATION = Animation<TextureRegion>(0f, gdxArrayOf())
    }
}

val Entity.animationCmp: AnimationComponent
    get() = this[AnimationComponent.MAPPER]
        ?: throw GdxRuntimeException("AnimationComponent for entity '$this' is null")
