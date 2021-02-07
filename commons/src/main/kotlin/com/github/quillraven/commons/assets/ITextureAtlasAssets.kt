package com.github.quillraven.commons.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas

interface ITextureAtlasAssets {
    val descriptor: AssetDescriptor<TextureAtlas>

    companion object {
        val EMPTY_TEXTURE_ATLAS_ASSET = object : ITextureAtlasAssets {
            override val descriptor = AssetDescriptor("", TextureAtlas::class.java)
        }
    }
}