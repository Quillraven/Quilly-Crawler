package com.github.quillraven.quillycrawler.ashley

import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import com.github.quillraven.commons.ashley.EntityConfigurations
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ai.PlayerState
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets

enum class EntityType {
    PLAYER,
    BIG_DEMON,
    CHEST,
}

class EntityConfiguration : AbstractEntityConfiguration()

fun loadEntityConfigurations(): EntityConfigurations<EntityConfiguration> =
    EntityConfigurations {
        config(EntityType.PLAYER.name) {
            atlasFilePath = TextureAtlasAssets.CHARACTERS_AND_PROPS.descriptor.fileName
            regionKey = "wizard-m"
            moveSpeed = 2.5f
            initialState = PlayerState.IDLE
            bodyType = BodyDef.BodyType.DynamicBody
            boundingBoxHeightPercentage = 0.2f
        }
        config(EntityType.BIG_DEMON.name) {
            atlasFilePath = TextureAtlasAssets.CHARACTERS_AND_PROPS.descriptor.fileName
            regionKey = "big-demon"
            size.set(0.75f, 0.75f)
            initialState = BigDemonState.RUN
        }
        config(EntityType.CHEST.name) {
            atlasFilePath = TextureAtlasAssets.CHARACTERS_AND_PROPS.descriptor.fileName
            regionKey = "chest"
            initialState = ChestState.IDLE
            bodyType = BodyDef.BodyType.StaticBody
        }
    }
