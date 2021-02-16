package com.github.quillraven.quillycrawler

import com.github.quillraven.commons.ashley.component.IEntityType
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets

// TODO -> convert to entity configuration DSL containing graphic, size, component data, etc...
enum class EntityType(override val atlasFilePath: String, override val regionKey: String) : IEntityType {
    WIZARD_MALE(TextureAtlasAssets.MONSTERS.descriptor.fileName, "wizard-m"),
    BIG_DEMON(TextureAtlasAssets.MONSTERS.descriptor.fileName, "big-demon"),
    CHEST(TextureAtlasAssets.MONSTERS.descriptor.fileName, "chest")
}