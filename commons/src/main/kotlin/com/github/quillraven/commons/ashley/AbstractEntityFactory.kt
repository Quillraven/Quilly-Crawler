package com.github.quillraven.commons.ashley

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
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

/**
 * Class to create [entities][Entity] and create and cache
 * [entity configurations][AbstractEntityConfiguration] of type [ConfigType].
 * It provides a DSL to support a type-safe creation of configuration objects.
 *
 * Use [configurations] and [config] to create, configure and add configurations to the cache.
 * The config id must be unique. Otherwise a [GdxRuntimeException] is thrown.
 *
 * Use [get] to retrieve an already stored configuration. If the configuration is not existing then a
 * [GdxRuntimeException] is thrown.
 *
 * Use [newEntity] to create and add a new [Entity] to the [engine] by using an already existing configuration.
 * If a [world] is specified then a [Box2DComponent] is initialized if needed.
 *
 * Use [configFactory] to define how a new instance of type [ConfigType] should be created within [config].
 *
 * Override [configureEntity] to add your own specific configurations after an entity was created.
 */
abstract class AbstractEntityFactory<ConfigType : AbstractEntityConfiguration>(
    val engine: Engine,
    @PublishedApi
    internal val configFactory: () -> ConfigType,
    val world: World? = null
) {
    @PublishedApi
    internal val configCache = ObjectMap<String, ConfigType>()

    /**
     * Creates and stores a new configuration with the given [id] using the [configFactory] function.
     * Throws a [GdxRuntimeException] if the [id] is already existing.
     */
    inline fun config(id: String, block: ConfigType.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }

        if (id in configCache) {
            throw GdxRuntimeException("Configuration of id '$id' already exists. Configurations must be unique!")
        }

        val newCfg = configFactory().apply(block)
        configCache[id] = newCfg
        LOG.debug { "Adding configuration '$id': $newCfg" }
    }

    /**
     * Returns the configuration of the given [id]. [config] must be called prior to a [get] call.
     * If the configuration does not exist then a [GdxRuntimeException] is thrown.
     */
    operator fun get(id: String): ConfigType {
        return configCache[id] ?: throw GdxRuntimeException("There is no configuration for id '$id'")
    }

    operator fun contains(id: String): Boolean = id in configCache

    /**
     * Creates and adds a new [Entity] to the [engine] using the configuration of [cfgId].
     * Sets the [TransformComponent.position] to [x] and [y].
     *
     * In case a box2d configuration is specified then a body is created for the [world].
     *
     * Configures following components:
     * - [TransformComponent]
     * - [AnimationComponent]
     * - [RenderComponent]
     * - [Box2DComponent]
     * - [StateComponent]
     *
     * Use [configureEntity] to add custom configuration to the created entity.
     */
    fun newEntity(
        x: Float,
        y: Float,
        cfgId: String
    ): Entity {
        LOG.debug { "Creating new entity with configuration '$cfgId'" }
        val entityCfg = configCache[cfgId]

        return engine.entity {
            // transform
            val transformCmp = with<TransformComponent> {
                position.set(x, y, position.z)
                size.set(entityCfg.size)
            }

            // animation and render
            if (entityCfg.atlasFilePath.isNotBlank()) {
                with<AnimationComponent> {
                    atlasFilePath = entityCfg.atlasFilePath
                    regionKey = entityCfg.regionKey
                }
                with<RenderComponent>()
            }

            // box2d
            val bodyType = entityCfg.bodyType
            if (world != null && bodyType != null) {
                with<Box2DComponent> {
                    body = world.body(bodyType) {
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

            // state
            if (entityCfg.initialState != EntityState.EMPTY_STATE) {
                with<StateComponent> {
                    state = entityCfg.initialState
                }
            }

            configureEntity(this, entityCfg)
        }
    }

    /**
     * This function is called at the end of [newEntity]. Use it to add custom configuration for your
     * created entities.
     *
     * It receives the created [engineEntity] and the [configuration] used for the creation process.
     */
    abstract fun configureEntity(engineEntity: EngineEntity, configuration: ConfigType)

    companion object {
        val LOG = logger<AbstractEntityFactory<AbstractEntityConfiguration>>()
    }
}

/**
 * Calls the specified function [block] on an [AbstractEntityFactory]. Use [AbstractEntityFactory.config] within
 * [block] to easily setup the configurations for your entities.
 */
fun <T : AbstractEntityFactory<out AbstractEntityConfiguration>> T.configurations(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return this.apply(block)
}
