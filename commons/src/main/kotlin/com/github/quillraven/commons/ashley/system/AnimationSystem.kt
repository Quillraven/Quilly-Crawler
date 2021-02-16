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
import com.github.quillraven.commons.collections.getOrPut
import kotlinx.coroutines.launch
import ktx.ashley.allOf
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.error
import ktx.log.info
import ktx.log.logger

/**
 * System to set and update an [entity's][Entity] [animation][Animation]. It caches animations internally to avoid
 * loading and creating the same animation again and again. It uses [TextureAtlas.findRegions] to find
 * [regions][TextureRegion] for an [Animation]. Therefore, use indexed region names in order for this system
 * to work properly.
 *
 * It also updates the [entity's][Entity] [RenderComponent] by setting the [TextureRegion]
 * and size of the [sprite][RenderComponent.sprite].
 *
 * Requires an [AssetStorage] that stores the different [atlas][TextureAtlas] objects that contain
 * the [regions][TextureRegion] for an animation.
 *
 * Requires a [unitScale] that defines the world unit to pixel ratio.
 *
 * The [defaultFrameDuration] is 10 frames per second and the [maxCacheSize] of the animation cache is 100.
 * Both values can be changed if necessary.
 */
class AnimationSystem(
    private val assetStorage: AssetStorage,
    private val unitScale: Float,
    private val defaultFrameDuration: Float = 1 / 10f,
    private val maxCacheSize: Int = 100
) : IteratingSystem(allOf(AnimationComponent::class, RenderComponent::class).get()) {
    private val animationCache = ObjectMap<String, ObjectMap<String, Animation<TextureRegion>>>(maxCacheSize)

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val animationCmp = entity.animationCmp

        // set or update animation
        if (animationCmp.dirty) {
            // some animation related properties changed -> get new animation
            animationCmp.gdxAnimation = cachedAnimation(animationCmp.atlasFilePath, animationCmp.regionKey)
        } else {
            animationCmp.stateTime += (deltaTime * animationCmp.animationSpeed)
        }

        // get current region (=keyFrame) of animation
        val keyFrame = if (animationCmp.gdxAnimation == AnimationComponent.EMPTY_ANIMATION) {
            // something went wrong when trying to set animation -> check log errors
            // we will render an error texture to visualize it in game
            errorRegion()
        } else {
            animationCmp.gdxAnimation.playMode = animationCmp.playMode
            animationCmp.gdxAnimation.getKeyFrame(animationCmp.stateTime)
        }

        // update the sprite's texture according to the current animation frame
        entity.renderCmp.sprite.run {
            val flipX = isFlipX
            val flipY = isFlipY
            setRegion(keyFrame)
            setSize(keyFrame.regionWidth * unitScale, keyFrame.regionHeight * unitScale)
            setOrigin(width * 0.5f, height * 0.5f)
            setFlip(flipX, flipY)
        }
    }

    /**
     * Makes sure that a [cache] does not exceed the given [AnimationSystem.maxCacheSize] size.
     */
    private fun validateCacheSize(cache: ObjectMap<String, *>) {
        if (cache.size >= maxCacheSize) {
            LOG.info { "Maximum animation cache size reached. Cache will be cleared now" }
            cache.clear()
        }
    }

    /**
     * Returns a cached [Animation] or creates and caches a new one, if it doesn't exist yet.
     */
    private fun cachedAnimation(atlasFilePath: String, regionKey: String): Animation<TextureRegion> {
        validateCacheSize(animationCache)
        val atlasAnimations = animationCache.getOrPut(atlasFilePath) {
            ObjectMap<String, Animation<TextureRegion>>(maxCacheSize)
        }

        validateCacheSize(atlasAnimations)
        return atlasAnimations.getOrPut(regionKey) {
            newGdxAnimation(atlasFilePath, regionKey)
        }
    }

    /**
     * Creates a new [Animation] from the [regionKey][] [regions][TextureRegion] of a given [atlasFilePath] atlas.
     */
    private fun newGdxAnimation(atlasFilePath: String, regionKey: String): Animation<TextureRegion> {
        val regions = atlasRegions(atlasFilePath, regionKey)

        if (regions.isEmpty) {
            LOG.error { "No regions available for animation: (atlasFilePath=${atlasFilePath}, regionKey=${regionKey})" }
            return AnimationComponent.EMPTY_ANIMATION
        }

        LOG.debug { "New animation: (atlasFilePath=${atlasFilePath}, regionKey=${regionKey})" }
        return Animation(defaultFrameDuration, regions)
    }

    /**
     * Returns the [regions][TextureRegion] to a given [regionKey] of the atlas [atlasFilePath].
     * If the [AnimationSystem.assetStorage] did not load the atlas yet then it will be lazily loaded by this function.
     */
    private fun atlasRegions(atlasFilePath: String, regionKey: String): Array<TextureAtlas.AtlasRegion> {
        return if (!assetStorage.isLoaded<TextureAtlas>(atlasFilePath)) {
            if (assetStorage.fileResolver.resolve(atlasFilePath).exists()) {
                LOG.error { "Atlas '${atlasFilePath}' not loaded yet! Will load it now lazily" }
                assetStorage.loadSync<TextureAtlas>(atlasFilePath).findRegions(regionKey)
            } else {
                LOG.error { "Invalid atlas '${atlasFilePath}'" }
                gdxArrayOf()
            }
        } else {
            assetStorage.get<TextureAtlas>(atlasFilePath).findRegions(regionKey)
        }
    }

    /**
     * Creates an error [TextureRegion] that will be used in case an [Animation] could not be set.
     * This region will then be rendered instead. It is a red square.
     */
    private fun errorRegion(): TextureRegion {
        val errorRegionKey = "commonsErrorRegion"
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