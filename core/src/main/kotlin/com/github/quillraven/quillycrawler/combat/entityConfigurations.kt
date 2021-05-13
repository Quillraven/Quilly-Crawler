package com.github.quillraven.quillycrawler.combat

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.quillycrawler.ashley.component.*
import ktx.ashley.EngineEntity
import ktx.ashley.with
import ktx.collections.set
import kotlin.math.floor

private fun EngineEntity.withTransformAndAnimation(
  origEntity: Entity,
  scale: Float,
  setPos: TransformComponent.() -> Unit
) {
  val transformCmp = origEntity.transformCmp
  with<TransformComponent> {
    size.set(transformCmp.size.x * scale, transformCmp.size.y * scale)
    this.apply(setPos)
  }
  val aniCmp = origEntity.animationCmp
  with<AnimationComponent> {
    this.atlasFilePath = aniCmp.atlasFilePath
    this.regionKey = aniCmp.regionKey
    this.stateKey = "idle"
    this.animationSpeed = 0f
  }
  with<RenderComponent>()
}

fun EngineEntity.configurePlayerCombatEntity(playerEntity: Entity, viewport: Viewport) {
  withTransformAndAnimation(playerEntity, 1.5f) {
    position.set(
      viewport.camera.position.x - size.x * 0.5f + 2f,
      viewport.camera.position.y - viewport.worldHeight * 0.5f + 0.5f,
      position.z
    )
  }
  with<PlayerComponent>()
  with<BagComponent> { playerEntity.bagCmp.items.forEach { entry -> items[entry.key] = entry.value } }
  with<GearComponent> { playerEntity.gearCmp.gear.forEach { entry -> gear[entry.key] = entry.value } }
  with<StatsComponent> { playerEntity.statsCmp.stats.forEach { entry -> stats[entry.key] = entry.value } }
  with<CombatComponent>()
  with<BuffComponent>()
}

fun EngineEntity.configureEnemyCombatEntity(
  enemyEntity: Entity,
  dungeonLevel: Int,
  viewport: Viewport,
  enemyIndex: Int,
  numEntities: Int
) {
  withTransformAndAnimation(enemyEntity, 1.25f) {
    val leftX = viewport.camera.position.x - viewport.worldWidth * 0.5f
    position.set(
      leftX + (viewport.worldWidth / (numEntities + 1)) * (enemyIndex + 1) - size.x * 0.5f,
      viewport.camera.position.y + viewport.worldHeight * 0.5f - 2.5f - MathUtils.random(0f, 1.1f),
      position.z
    )
  }
  with<CombatComponent>()
  with<BuffComponent>()

  val statsCmp: StatsComponent = when (enemyEntity.tiledCmp.name) {
    "GOBLIN" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 7f
        stats[StatsType.AGILITY] = 5f
        stats[StatsType.PHYSICAL_DAMAGE] = 3f
        stats[StatsType.PHYSICAL_ARMOR] = 1f
        stats[StatsType.MAGIC_ARMOR] = 1f
      }
    }
    "SKELET" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 15f
        stats[StatsType.AGILITY] = 3f
        stats[StatsType.PHYSICAL_DAMAGE] = 5f
      }
    }
    "IMP" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 5f
        stats[StatsType.AGILITY] = 15f
        stats[StatsType.PHYSICAL_DAMAGE] = 4f
        stats[StatsType.PHYSICAL_ARMOR] = 3f
        stats[StatsType.MAGIC_ARMOR] = 5f
      }
    }
    "CHORT" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 22f
        stats[StatsType.AGILITY] = 8f
        stats[StatsType.PHYSICAL_DAMAGE] = 6f
        stats[StatsType.PHYSICAL_ARMOR] = 7f
      }
    }
    "BIG_DEMON" -> {
      with<CombatAIComponent> { treeFilePath = "ai/genericCombat.tree" }
      with {
        stats[StatsType.LIFE] = 50f
        stats[StatsType.AGILITY] = 11f
        stats[StatsType.PHYSICAL_DAMAGE] = 7f
        stats[StatsType.MAGIC_DAMAGE] = 6f
        stats[StatsType.PHYSICAL_ARMOR] = 4f
        stats[StatsType.MAGIC_ARMOR] = 4f
      }
    }
    else -> {
      throw GdxRuntimeException("Unsupported enemy combat name: ${enemyEntity.tiledCmp.name}")
    }
  }

  // adjusts stats according to dungeon level: each level increases stats by 5%
  statsCmp.stats.forEach { entry -> statsCmp[entry.key] = floor(entry.value * (1f + (dungeonLevel - 1) * 0.05f)) }
  statsCmp[StatsType.MAX_LIFE] = statsCmp[StatsType.LIFE]
  statsCmp[StatsType.MAX_MANA] = statsCmp[StatsType.MANA]
}
