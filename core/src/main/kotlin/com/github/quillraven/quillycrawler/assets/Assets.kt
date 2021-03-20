package com.github.quillraven.quillycrawler.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.I18NBundle

enum class TextureAtlasAssets(
  filePath: String,
  val descriptor: AssetDescriptor<TextureAtlas> = AssetDescriptor(filePath, TextureAtlas::class.java)
) {
  ENTITIES("graphics/entities.atlas"),
  UI("graphics/ui.atlas")
}

enum class I18NAssets(
  filePath: String,
  val descriptor: AssetDescriptor<I18NBundle> = AssetDescriptor(
    filePath,
    I18NBundle::class.java
  )
) {
  DEFAULT("i18n")
}
