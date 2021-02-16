package com.github.quillraven.commons.collections

import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.set

// TODO -> make PR for LibKTX
inline fun <K, V> ObjectMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return if (this.containsKey(key)) {
        this[key]
    } else {
        val newValue = defaultValue()
        this[key] = newValue
        return newValue
    }
}