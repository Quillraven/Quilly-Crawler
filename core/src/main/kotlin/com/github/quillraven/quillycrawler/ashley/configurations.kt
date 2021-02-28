package com.github.quillraven.quillycrawler.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import com.github.quillraven.commons.ashley.EntityConfigurations
import com.github.quillraven.commons.ashley.component.CameraLockComponent
import com.github.quillraven.commons.ashley.component.PlayerComponent
import com.github.quillraven.commons.ashley.component.box2dCmp
import com.github.quillraven.commons.ashley.component.transformCmp
import com.github.quillraven.quillycrawler.ai.BigDemonState
import com.github.quillraven.quillycrawler.ai.ChestState
import com.github.quillraven.quillycrawler.ai.PlayerState
import com.github.quillraven.quillycrawler.ashley.component.CollectableComponent
import com.github.quillraven.quillycrawler.ashley.component.CollectingComponent
import com.github.quillraven.quillycrawler.ashley.component.MoveComponent
import com.github.quillraven.quillycrawler.ashley.component.PlayerControlComponent
import com.github.quillraven.quillycrawler.assets.TextureAtlasAssets
import ktx.ashley.EngineEntity
import ktx.ashley.with
import ktx.box2d.circle

enum class EntityType {
    PLAYER,
    BIG_DEMON,
    CHEST,
}

class EntityConfiguration(
    var playerControlled: Boolean = false,
    var actionable: Boolean = false
) : AbstractEntityConfiguration()

class CustomEntityConfigurations(block: EntityConfigurations<EntityConfiguration>.() -> Unit = {}) :
    EntityConfigurations<EntityConfiguration>({ EntityConfiguration() }, block) {
    override fun newEntity(
        engine: Engine,
        x: Float,
        y: Float,
        cfgId: String,
        world: World?,
        configure: EngineEntity.(EntityConfiguration) -> Unit
    ): Entity {
        return super.newEntity(engine, x, y, cfgId, world) { entityCfg ->
            apply { configure(entityCfg) }

            if (entityCfg.playerControlled) {
                with<PlayerComponent>()
                with<PlayerControlComponent>()
                with<CollectingComponent>()
                this.entity.box2dCmp.body.circle(this.entity.transformCmp.size.x) {
                    isSensor = true
                }
                with<MoveComponent> {
                    maxSpeed = entityCfg.moveSpeed
                }
                with<CameraLockComponent>()
            }

            if (entityCfg.actionable) {
                with<CollectableComponent>()
            }
        }
    }
}

fun loadEntityConfigurations(): CustomEntityConfigurations =
    CustomEntityConfigurations {
        config(EntityType.PLAYER.name) {
            playerControlled = true
            atlasFilePath = TextureAtlasAssets.CHARACTERS_AND_PROPS.descriptor.fileName
            regionKey = "wizard-m"
            moveSpeed = 5f
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
            actionable = true
        }
    }
