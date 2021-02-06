package com.github.quillraven.commons.ashley.system

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import kotlinx.coroutines.launch
import ktx.ashley.allOf
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.collections.set
import ktx.log.debug
import ktx.log.error
import ktx.log.logger

inline fun <K, V> ObjectMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return if (this.containsKey(key)) {
        this[key]
    } else {
        val newValue = defaultValue()
        this[key] = newValue
        return newValue
    }
}

class AnimationSystem(
    private val assetStorage: AssetStorage,
    private val unitScale: Float,
    private val framesPerSecond: Float,
) : IteratingSystem(
    allOf(
        AnimationComponent::class,
        RenderComponent::class
    ).get()
) {
    private val animationCache =
        ObjectMap<ITextureAtlasAssets, ObjectMap<IAnimationType, Animation<TextureRegion>>>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val animation = entity.animation

        if (animation.dirty) {
            // animation type changed -> set new animation
            animation.gdxAnimation = gdxAnimation(animation.type)
        } else {
            animation.stateTime += deltaTime
        }

        val keyFrame = if (animation.gdxAnimation == AnimationComponent.EMPTY_ANIMATION) {
            // something went wrong when trying to set animation -> check log errors
            // we will render an error texture to visualize it in game
            errorRegion()
        } else {
            animation.gdxAnimation.getKeyFrame(animation.stateTime)
        }

        entity.render.sprite.run {
            val flipX = isFlipX
            val flipY = isFlipY
            setRegion(keyFrame)
            setSize(keyFrame.regionWidth * unitScale, keyFrame.regionHeight * unitScale)
            setOrigin(width * 0.5f, height * 0.5f)
            setFlip(flipX, flipY)
        }
    }

    private fun gdxAnimation(animationType: IAnimationType): Animation<TextureRegion> {
        val atlasAnimations = animationCache.getOrPut(animationType.atlasAsset) {
            ObjectMap<IAnimationType, Animation<TextureRegion>>()
        }

        return atlasAnimations.getOrPut(animationType) {
            newGdxAnimation(animationType)
        }
    }

    private fun newGdxAnimation(animationType: IAnimationType): Animation<TextureRegion> {
        val regions = atlasRegions(animationType)

        if (regions.isEmpty) {
            LOG.error { "Invalid animation: (atlasAsset=${animationType.atlasAsset.descriptor.fileName}, atlasKey=${animationType.atlasKey})" }
            return AnimationComponent.EMPTY_ANIMATION
        }

        LOG.debug { "New animation: (atlasAsset=${animationType.atlasAsset.descriptor.fileName}, atlasKey=${animationType.atlasKey})" }
        return Animation(framesPerSecond * animationType.speed, regions, animationType.playMode)
    }

    private fun atlasRegions(animationType: IAnimationType): Array<TextureAtlas.AtlasRegion> {
        return if (!assetStorage.isLoaded(animationType.atlasAsset.descriptor)) {
            if (assetStorage.fileResolver.resolve(animationType.atlasAsset.descriptor.fileName).exists()) {
                LOG.error { "Atlas '${animationType.atlasAsset.descriptor.fileName}' not loaded yet! Will lazy load it now" }
                assetStorage.loadSync(animationType.atlasAsset.descriptor).findRegions(animationType.atlasKey)
            } else {
                LOG.error { "Invalid atlas '${animationType.atlasAsset.descriptor.fileName}'" }
                gdxArrayOf()
            }
        } else {
            assetStorage[animationType.atlasAsset.descriptor].findRegions(animationType.atlasKey)
        }
    }

    private fun errorRegion(): TextureRegion {
        val errorRegionKey = "errorRegion"
        if (!assetStorage.isLoaded<TextureRegion>(errorRegionKey)) {
            LOG.debug { "Creating error TextureRegion" }
            KtxAsync.launch {
                val pixmap = Pixmap((1 / unitScale).toInt(), (1 / unitScale).toInt(), Pixmap.Format.RGB888).apply {
                    setColor(1f, 0f, 0f, 1f)
                    fill()
                }
                val texture = Texture(pixmap)

                assetStorage.add("${errorRegionKey}Pixmap", pixmap)
                assetStorage.add("${errorRegionKey}Texture", texture)
                assetStorage.add(errorRegionKey, TextureRegion(texture))
            }
        }

        return assetStorage[errorRegionKey]
    }

    companion object {
        private val LOG = logger<AnimationSystem>()
    }
}