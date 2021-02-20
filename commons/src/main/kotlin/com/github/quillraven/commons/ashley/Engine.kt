package com.github.quillraven.commons.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.EngineEntity
import ktx.ashley.entity
import ktx.ashley.with
import ktx.box2d.body
import ktx.box2d.box

fun Engine.entityByCfg(
    x: Float,
    y: Float,
    cfgId: String,
    configurations: EntityConfigurations<out AbstractEntityConfiguration>,
    world: World? = null,
    configure: EngineEntity.() -> Unit = {}
): Entity {
    val entityCfg = configurations[cfgId]

    return this.entity {
        with<EntityConfigurationComponent> {
            config = entityCfg
        }

        val transformCmp = with<TransformComponent> {
            position.set(x, y, position.z)
            size.set(entityCfg.size)
        }

        if (entityCfg.atlasFilePath.isNotBlank()) {
            with<AnimationComponent> {
                atlasFilePath = entityCfg.atlasFilePath
                regionKey = entityCfg.regionKey
            }
            with<RenderComponent>()
        }

        if (world != null && entityCfg.bodyType != null) {
            with<Box2DComponent> {
                body = world.body(BodyDef.BodyType.DynamicBody) {
                    position.set(
                        transformCmp.position.x + transformCmp.size.x * 0.5f,
                        transformCmp.position.y + transformCmp.size.y * 0.5f
                    )
                    fixedRotation = true
                    allowSleep = false
                    val boundingBoxHeight = transformCmp.size.y * entityCfg.boundingBoxHeightPercentage
                    box(
                        transformCmp.size.x,
                        boundingBoxHeight,
                        Vector2(0f, -transformCmp.size.y * 0.5f + boundingBoxHeight * 0.5f)
                    ) {
                        friction = 0f
                    }

                    userData = this@entity.entity
                }
            }
        }

        if (entityCfg.initialState != EntityState.EMPTY_STATE) {
            with<StateComponent> {
                state = entityCfg.initialState
            }
        }

        apply(configure)
    }
}