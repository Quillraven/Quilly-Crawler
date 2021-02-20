package com.github.quillraven.commons.ashley

import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
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

        if (configurations.containsKey(id)) {
            throw GdxRuntimeException("Configuration of id '$id' already exists. Configurations must be unique!")
        }

        val newCfg = type.createInstance().apply(block)
        configurations[id] = newCfg
        LOG.debug { "Adding configuration '$id': $newCfg" }
    }

    operator fun get(id: String): AbstractEntityConfiguration {
        return configurations[id] ?: throw GdxRuntimeException("There is no configuration for id '$id'")
    }

    companion object {
        val LOG = logger<EntityConfigurations<*>>()

        inline operator fun <reified ConfigType : AbstractEntityConfiguration> invoke(block: EntityConfigurations<ConfigType>.() -> Unit) =
            EntityConfigurations(ConfigType::class).apply(block)
    }
}
