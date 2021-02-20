package com.github.quillraven.commons.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.github.quillraven.commons.ashley.component.*
import ktx.ashley.EngineEntity
import ktx.ashley.entity
import ktx.ashley.with
import ktx.box2d.body
import ktx.box2d.box
import ktx.collections.contains
import ktx.collections.set
import ktx.log.debug
import ktx.log.logger
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class EntityConfigurations<ConfigType : AbstractEntityConfiguration>
@PublishedApi
internal constructor(
    @PublishedApi
    internal val type: KClass<ConfigType>
) {
    @PublishedApi
    internal val configurations = ObjectMap<String, ConfigType>()

    inline fun config(id: String, block: ConfigType.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }

        if (id in configurations) {
            throw GdxRuntimeException("Configuration of id '$id' already exists. Configurations must be unique!")
        }

        val newCfg = type.createInstance().apply(block)
        configurations[id] = newCfg
        LOG.debug { "Adding configuration '$id': $newCfg" }
    }

    operator fun get(id: String): ConfigType {
        return configurations[id] ?: throw GdxRuntimeException("There is no configuration for id '$id'")
    }

    fun newEntity(
        engine: Engine,
        x: Float,
        y: Float,
        cfgId: String,
        world: World? = null,
        configure: EngineEntity.(ConfigType) -> Unit = {}
    ): Entity {
        val entityCfg = configurations[cfgId]

        return engine.entity {
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

            apply { configure(entityCfg) }
        }
    }

    companion object {
        val LOG = logger<EntityConfigurations<AbstractEntityConfiguration>>()

        inline operator fun <reified ConfigType : AbstractEntityConfiguration> invoke(block: EntityConfigurations<ConfigType>.() -> Unit) =
            EntityConfigurations(ConfigType::class).apply(block)
    }
}
