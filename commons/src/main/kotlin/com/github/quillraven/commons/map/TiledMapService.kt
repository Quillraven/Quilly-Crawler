package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.objects.*
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.AbstractEntityConfiguration
import com.github.quillraven.commons.ashley.AbstractEntityFactory
import com.github.quillraven.commons.ashley.component.*
import kotlinx.coroutines.launch
import ktx.ashley.allOf
import ktx.ashley.configureEntity
import ktx.ashley.entity
import ktx.ashley.with
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.box2d.*
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.error
import ktx.tiled.*
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class TiledMapService(
  private val entityFactory: AbstractEntityFactory<out AbstractEntityConfiguration>,
  assetStorage: AssetStorage,
  batch: Batch,
  private val unitScale: Float,
  override val mapRenderer: OrthogonalTiledMapRenderer = OrthogonalTiledMapRenderer(null, unitScale, batch)
) : MapService(assetStorage, entityFactory.engine) {
  override val mapEntities: ImmutableArray<Entity> = engine.getEntitiesFor(allOf(TiledComponent::class).get())

  private var currentMapFilePath = ""
  private var currentMap: TiledMap = EMPTY_MAP
  private val backgroundLayers = gdxArrayOf<TiledMapTileLayer>()
  private val foregroundLayers = gdxArrayOf<TiledMapTileLayer>()

  init {
    assetStorage.setLoader<TiledMap> { TmxMapLoader(assetStorage.fileResolver) }
  }

  override fun setMap(engine: Engine, mapFilePath: String) {
    if (!assetStorage.fileResolver.resolve(mapFilePath).exists()) {
      throw GdxRuntimeException("Map '$mapFilePath' does not exist!")
    }

    KtxAsync.launch {
      if (currentMap != EMPTY_MAP) {
        // unload current map and remove map entities
        listeners.forEach { it.beforeMapChange(this@TiledMapService, currentMap) }
        assetStorage.unload<TiledMap>(currentMapFilePath)
        LOG.debug { "Removing ${mapEntities.size()} map entities" }
        mapEntities.forEach { it.removeFromEngine(engine) }
      }

      // load new map
      if (Gdx.app.logLevel == Application.LOG_DEBUG) {
        LOG.debug {
          "Loading of map $mapFilePath took '${
            measureTimeMillis {
              currentMap = assetStorage.loadSync(mapFilePath)
            }
          }' ms"
        }
      } else {
        currentMap = assetStorage.loadSync(mapFilePath)
      }

      // and create map entities like collision entities and game object entities
      val currentSize = mapEntities.size()
      parseRenderLayers()
      parseObjectLayers()
      LOG.debug { "Created ${mapEntities.size() - currentSize} map entities" }

      currentMapFilePath = mapFilePath
      mapRenderer.map = currentMap
      listeners.forEach { it.afterMapChange(this@TiledMapService, currentMap) }
    }
  }

  private fun parseRenderLayers() {
    backgroundLayers.clear()
    foregroundLayers.clear()
    currentMap.forEachLayer<TiledMapTileLayer> { layer ->
      if (layer.property(Z_PROPERTY, Z_DEFAULT) <= Z_DEFAULT) {
        backgroundLayers.add(layer)
      } else {
        foregroundLayers.add(layer)
      }
    }
  }

  private fun parseObjectLayers() {
    currentMap.forEachLayer<MapLayer> { layer ->
      if (layer.property(COLLISION_LAYER_PROPERTY, false)) {
        createCollisionBody(layer)
      } else {
        createEntities(layer.objects)
      }
    }
  }

  private fun createCollisionBody(layer: MapLayer) {
    val objects = layer.objects
    if (entityFactory.world == null || objects.isEmpty()) {
      LOG.debug {
        "There is no world (${entityFactory.world}) specified or " +
          "there are no collision objects for layer '${layer.name}'"
      }
      return
    }

    engine.entity {
      with<TiledComponent>()
      with<Box2DComponent> {
        body = entityFactory.world.body(BodyDef.BodyType.StaticBody) {
          fixedRotation = true

          objects.forEach { mapObject ->
            when (val shape = mapObject.shape) {
              is Polyline -> {
                shape.setPosition(shape.x * unitScale, shape.y * unitScale)
                shape.setScale(unitScale, unitScale)
                chain(shape.transformedVertices)
              }
              is Polygon -> {
                shape.setPosition(shape.x * unitScale, shape.y * unitScale)
                shape.setScale(unitScale, unitScale)
                loop(shape.transformedVertices)
              }
              is Rectangle -> {
                val x = shape.x * unitScale
                val y = shape.y * unitScale
                val width = shape.width * unitScale
                val height = shape.height * unitScale

                if (width <= 0f || height <= 0f) {
                  LOG.error { "MapObject '${mapObject.id}' on layer '${layer.name}' is a rectangle with zero width or height" }
                  return@forEach
                }

                // define loop vertices
                // bottom left corner
                TMP_RECTANGLE_VERTICES[0] = x
                TMP_RECTANGLE_VERTICES[1] = y
                // top left corner
                TMP_RECTANGLE_VERTICES[2] = x
                TMP_RECTANGLE_VERTICES[3] = y + height
                // top right corner
                TMP_RECTANGLE_VERTICES[4] = x + width
                TMP_RECTANGLE_VERTICES[5] = y + height
                // bottom right corner
                TMP_RECTANGLE_VERTICES[6] = x + width
                TMP_RECTANGLE_VERTICES[7] = y

                loop(TMP_RECTANGLE_VERTICES)
              }
              else -> {
                LOG.error { "MapObject '${mapObject.id}' has an unsupported collision type: ${shape::class.simpleName}" }
              }
            }
          }

          userData = this@entity.entity
        }
      }
    }
  }

  private fun createEntities(objects: MapObjects) {
    objects.forEach { mapObject ->
      val cfgId = mapObject.name ?: ""
      if (cfgId !in entityFactory) {
        LOG.error { "MapObject '${mapObject.id}' has a name '${cfgId}' that does not match any entity configuration" }
        return@forEach
      }

      entityFactory.newEntity(
        mapObject.x * unitScale,
        mapObject.y * unitScale,
        cfgId
      ).also { entity ->
        engine.configureEntity(entity) {
          with<TiledComponent> {
            id = mapObject.id
          }
        }

        listeners.forEach { it.onMapEntityCreation(entity) }
      }
    }
  }

  override fun setViewBounds(camera: OrthographicCamera) {
    // update animation tiles
    AnimatedTiledMapTile.updateAnimationBaseTime()

    // set view bounds
    val width: Float = camera.viewportWidth * camera.zoom
    val height: Float = camera.viewportHeight * camera.zoom
    val w: Float = width * abs(camera.up.y) + height * abs(camera.up.x)
    val h: Float = height * abs(camera.up.y) + width * abs(camera.up.x)
    mapRenderer.viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
  }

  override fun renderBackground() {
    backgroundLayers.forEach { mapRenderer.renderTileLayer(it) }
  }

  override fun renderForeground() {
    foregroundLayers.forEach { mapRenderer.renderTileLayer(it) }
  }

  companion object {
    const val Z_PROPERTY = "z"
    const val COLLISION_LAYER_PROPERTY = "collisionLayer"
    private val EMPTY_MAP: TiledMap = TiledMap()
    private val TMP_RECTANGLE_VERTICES = FloatArray(8)
  }
}
