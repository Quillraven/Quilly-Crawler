package com.github.quillraven.quillycrawler.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas

enum class TextureAtlasAssets(
  filePath: String,
  val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor(filePath, TextureAtlas::class.java)
) {
  ENTITIES("graphics/entities.atlas")
}
