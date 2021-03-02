package com.github.quillraven.commons.ashley.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool

class TiledComponent : Component, Pool.Poolable {
    var id: Int = -1

    override fun reset() {
        id = -1
    }
}
