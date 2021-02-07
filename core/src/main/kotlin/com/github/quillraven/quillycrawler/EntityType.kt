package com.github.quillraven.quillycrawler

import com.github.quillraven.commons.ashley.component.IEntityType
import com.github.quillraven.commons.assets.ITextureAtlasAssets
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets

enum class EntityType(override val atlasAsset: ITextureAtlasAssets, override val regionKey: String) : IEntityType {
    WIZARD_MALE(TextureAtlasAssets.MONSTERS, "wizard-m"),
    BIG_DEMON(TextureAtlasAssets.MONSTERS, "big-demon")
}