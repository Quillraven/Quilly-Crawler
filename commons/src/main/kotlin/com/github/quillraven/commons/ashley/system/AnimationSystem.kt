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
import com.github.quillraven.commons.ashley.component.AnimationComponent
import com.github.quillraven.commons.ashley.component.RenderComponent
import com.github.quillraven.commons.ashley.component.animationCmp
import com.github.quillraven.commons.ashley.component.renderCmp
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
        ObjectMap<ITextureAtlasAssets, ObjectMap<String, Animation<TextureRegion>>>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val animationCmp = entity.animationCmp

        if (animationCmp.dirty) {
            // animation type changed -> set new animation
            animationCmp.gdxAnimation = gdxAnimation(animationCmp.atlasAsset, animationCmp.regionKey)
        } else {
            animationCmp.stateTime += (deltaTime * animationCmp.animationSpeed)
        }

        val keyFrame = if (animationCmp.gdxAnimation == AnimationComponent.EMPTY_ANIMATION) {
            // something went wrong when trying to set animation -> check log errors
            // we will render an error texture to visualize it in game
            errorRegion()
        } else {
            animationCmp.gdxAnimation.getKeyFrame(animationCmp.stateTime)
        }

        entity.renderCmp.sprite.run {
            val flipX = isFlipX
            val flipY = isFlipY
            setRegion(keyFrame)
            setSize(keyFrame.regionWidth * unitScale, keyFrame.regionHeight * unitScale)
            setOrigin(width * 0.5f, height * 0.5f)
            setFlip(flipX, flipY)
        }
    }

    private fun gdxAnimation(atlasAsset: ITextureAtlasAssets, regionKey: String): Animation<TextureRegion> {
        val atlasAnimations = animationCache.getOrPut(atlasAsset) {
            ObjectMap<String, Animation<TextureRegion>>()
        }

        return atlasAnimations.getOrPut(regionKey) {
            newGdxAnimation(atlasAsset, regionKey)
        }
    }

    private fun newGdxAnimation(atlasAsset: ITextureAtlasAssets, regionKey: String): Animation<TextureRegion> {
        val regions = atlasRegions(atlasAsset, regionKey)

        if (regions.isEmpty) {
            LOG.error { "Invalid animation: (atlasAsset=${atlasAsset.descriptor.fileName}, atlasKey=${regionKey})" }
            return AnimationComponent.EMPTY_ANIMATION
        }

        LOG.debug { "New animation: (atlasAsset=${atlasAsset.descriptor.fileName}, atlasKey=${regionKey})" }
        return Animation(framesPerSecond, regions, Animation.PlayMode.LOOP)
    }

    private fun atlasRegions(atlasAsset: ITextureAtlasAssets, regionKey: String): Array<TextureAtlas.AtlasRegion> {
        return if (!assetStorage.isLoaded(atlasAsset.descriptor)) {
            if (assetStorage.fileResolver.resolve(atlasAsset.descriptor.fileName).exists()) {
                LOG.error { "Atlas '${atlasAsset.descriptor.fileName}' not loaded yet! Will lazy load it now" }
                assetStorage.loadSync(atlasAsset.descriptor).findRegions(regionKey)
            } else {
                LOG.error { "Invalid atlas '${atlasAsset.descriptor.fileName}'" }
                gdxArrayOf()
            }
        } else {
            assetStorage[atlasAsset.descriptor].findRegions(regionKey)
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