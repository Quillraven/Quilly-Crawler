package com.github.quillraven.quillycrawler.ashley.component

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.quillraven.commons.ashley.component.IAnimationType
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets

enum class AnimationType(
    override val atlasAsset: ITextureAtlasAssets,
    override val atlasKey: String,
    override val speed: Float = 1f,
    override val playMode: Animation.PlayMode = Animation.PlayMode.LOOP
) : IAnimationType {
    BIG_DEMON_IDLE(TextureAtlasAssets.MONSTERS, "big-demon-idle"),
    BIG_DEMON_RUN(TextureAtlasAssets.MONSTERS, "big-demon-run")
}
