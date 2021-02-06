package com.github.quillraven.quillycrawler.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.github.quillraven.commons.assets.ITextureAtlasAssets

enum class TextureAtlasAssets(
    filePath: String,
    override val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor(filePath, TextureAtlas::class.java)
) : ITextureAtlasAssets {
    MONSTERS("graphics/monsters.atlas")
}
