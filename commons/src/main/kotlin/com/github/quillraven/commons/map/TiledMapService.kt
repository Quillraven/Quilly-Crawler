package com.github.quillraven.commons.map

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.GdxRuntimeException
import com.github.quillraven.commons.ashley.component.*
import com.github.quillraven.commons.audio.AudioService
import com.github.quillraven.commons.audio.DefaultAudioService
import com.github.quillraven.commons.map.MapService.Companion.LOG
import com.github.quillraven.commons.map.TiledMapService.Companion.COLLISION_LAYER_PROPERTY
import com.github.quillraven.commons.map.TiledMapService.Companion.MUSIC_FILE_PATH_PROPERTY
import com.github.quillraven.commons.map.TiledMapService.Companion.Z_PROPERTY
import kotlinx.coroutines.launch
import ktx.ashley.*
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.box2d.body
import ktx.box2d.chain
import ktx.box2d.loop
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.error
import ktx.tiled.*
import kotlin.math.abs
import kotlin.system.measureTimeMillis

/**
 * Implementation of [MapService] for Tiled map editor support. It uses an [Engine] to create [entities][Entity]
 * out of map [objects][MapObject]. It uses an [OrthogonalTiledMapRenderer] for rendering. The renderer will be
 * added to the [assetStorage] to dispose it once the [assetStorage] gets disposed. Also, a [TmxMapLoader]
 * will be set for the [assetStorage].
 *
 * [TiledMapTileLayer] layers with a [Z_PROPERTY] value lower equal [Z_DEFAULT] or without the [Z_PROPERTY] property
 * are background layers. Other [TiledMapTileLayer] layers are foreground layers.
 *
 * Use [configureEntity] to define a function that creates your game specific entities out of map objects.
 *
 * When a [world] is passed to the service and the map contains a layer with a boolean [property][COLLISION_LAYER_PROPERTY]
 * set to true then a single [Entity] is created with a [Box2DComponent] that contains all shapes of that layer
 * for the collision objects.
 *
 * Additionally, any entity created by this service gets a [TiledComponent] to mark it as a tiled map entity. Whenever
 * [setMap] is called then all entities with a [TiledComponent] get removed from the [Engine].
 *
 * If a [TiledMap] contains a [MUSIC_FILE_PATH_PROPERTY] then the [AudioService.playMusic] method of the given
 * [audioService] is called.
 */
class TiledMapService(
  private val assetStorage: AssetStorage,
  batch: Batch,
  private val unitScale: Float,
  private val world: World? = null,
  private val audioService: AudioService = DefaultAudioService,
  private val configureEntity: EngineEntity.(MapObject, World?) -> Boolean
) : MapService {
  private val mapRenderer = OrthogonalTiledMapRenderer(null, unitScale, batch)
  private val tiledEntitiesFamily = allOf(TiledComponent::class).get()
  private var currentMapFilePath = ""
  private var currentMap: TiledMap = EMPTY_MAP
  private val backgroundLayers = gdxArrayOf<TiledMapTileLayer>()
  private val foregroundLayers = gdxArrayOf<TiledMapTileLayer>()

  init {
    assetStorage.setLoader<TiledMap> { TmxMapLoader(assetStorage.fileResolver) }
    KtxAsync.launch {
      assetStorage.add("mapServiceMapRenderer", mapRenderer)
    }
  }

  /**
   * Unloads the previous map - if any - from the [assetStorage] and loads the new map. It also removes
   * any [entities][Entity] from the [engine] that have a [TiledComponent].
   *
   * Updates the foreground and background layers for rendering and calls [configureEntity] for every [MapObject]
   * of an object layer in Tiled. If a [world] is defined then also an entity with a [Box2DComponent] is created
   * that represents the collision shapes if there are layers with a [COLLISION_LAYER_PROPERTY] set to true.
   *
   * Plays music if the [MUSIC_FILE_PATH_PROPERTY] is specified.
   */
  override fun setMap(engine: Engine, mapFilePath: String) {
    if (!assetStorage.fileResolver.resolve(mapFilePath).exists()) {
      throw GdxRuntimeException("Map '$mapFilePath' does not exist!")
    }

    KtxAsync.launch {
      val mapEntities = engine.getEntitiesFor(tiledEntitiesFamily)

      if (currentMap != EMPTY_MAP) {
        // unload current map and remove map entities
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
      updateRenderLayers()
      parseObjectLayers(engine)
      updateMusic()

      currentMapFilePath = mapFilePath
      mapRenderer.map = currentMap
    }
  }

  /**
   * Updates [backgroundLayers] and [foregroundLayers]. Any [TiledMapTileLayer] without a [Z_PROPERTY] or
   * with a [Z_PROPERTY] value less or equal to [Z_DEFAULT] is a background layer. Otherwise it is a foreground layer.
   */
  private fun updateRenderLayers() {
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

  /**
   * Calls [configureEntity] for any [MapObject] of object layers in Tiled and creates the collision body
   * [Entity] if a [world] is defined and a layers with a [COLLISION_LAYER_PROPERTY] set to true exists.
   *
   * Any entity created by this function has a [TiledComponent] with the id of the object in Tiled.
   */
  private fun parseObjectLayers(engine: Engine) {
    currentMap.forEachLayer<MapLayer> { layer ->
      if (layer.property(COLLISION_LAYER_PROPERTY, false)) {
        createCollisionBody(engine, layer)
      } else {
        var engineEntity = EngineEntity(engine, engine.createEntity())
        var newEntityRequired = false
        var numCreated = 0

        // call 'configureEntity' for every MapObject
        layer.objects.forEach {
          newEntityRequired = engineEntity.configureEntity(it, world)
          if (newEntityRequired) {
            ++numCreated
            // entity was successfully configured -> add TiledComponent to keep it inside mapEntities array and
            // to automatically remove those entities when setMap gets called
            engineEntity.with<TiledComponent> {
              id = it.id
            }
            engine.addEntity(engineEntity.entity)
            // create new entity for next 'configureEntity' call
            engineEntity = EngineEntity(engine, engine.createEntity())
          }
        }

        if (!newEntityRequired) {
          --numCreated
          // the last call to 'configureEntity' returned false and the entity was not successfully configured.
          // -> Remove it from the engine
          engineEntity.with<RemoveComponent>()
        }

        LOG.debug { "Created $numCreated map entities" }
      }
    }
  }

  /**
   * Creates an [Entity] with a [Box2DComponent] out of the given [layer]. Any shape of the [layer] will be
   * added to the [body][Box2DComponent.body] of the entity.
   */
  private fun createCollisionBody(engine: Engine, layer: MapLayer) {
    val objects = layer.objects
    if (world == null || objects.isEmpty()) {
      LOG.debug {
        "There is no world (${world}) specified or " +
          "there are no collision objects for layer '${layer.name}'"
      }
      return
    }

    engine.entity {
      // we cannot set an id because there could be multiple objects in Tiled that are used for the collision
      // and therefore we do not have a unique id
      with<TiledComponent>()
      // create collision body
      with<Box2DComponent> {
        body = world.body(BodyDef.BodyType.StaticBody) {
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

  /**
   * Reads the [MUSIC_FILE_PATH_PROPERTY] of the map and calls the [AudioService.playMusic] method
   * of the [audioService] if it is defined.
   */
  private fun updateMusic() {
    val musicFilePath = currentMap.property(MUSIC_FILE_PATH_PROPERTY, "")
    if (musicFilePath.isNotBlank()) {
      if (assetStorage.fileResolver.resolve(musicFilePath).exists()) {
        audioService.playMusic(musicFilePath)
      } else {
        LOG.error { "Music file path '$musicFilePath' does not exist!" }
      }
    }
  }

  /**
   * Sets the view bounds of the [mapRenderer] by the given [camera].
   * Also, makes a call to [AnimatedTiledMapTile.updateAnimationBaseTime] to update map animations.
   */
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

  /**
   * Renders all [backgroundLayers]-
   */
  override fun renderBackground() {
    backgroundLayers.forEach { mapRenderer.renderTileLayer(it) }
  }

  /**
   * Renders all [foregroundLayers].
   */
  override fun renderForeground() {
    foregroundLayers.forEach { mapRenderer.renderTileLayer(it) }
  }

  companion object {
    const val Z_PROPERTY = "z"
    const val COLLISION_LAYER_PROPERTY = "collisionLayer"
    const val MUSIC_FILE_PATH_PROPERTY = "musicFilePath"
    private val EMPTY_MAP: TiledMap = TiledMap()
    private val TMP_RECTANGLE_VERTICES = FloatArray(8)
  }
}
