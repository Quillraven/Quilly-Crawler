dependencies {
    implementation("com.badlogicgames.gdx:gdx:${project.property("gdxVersion")}")
    implementation("com.badlogicgames.gdx:gdx-box2d:${project.property("gdxVersion")}")
    implementation("com.badlogicgames.ashley:ashley:${project.property("ashleyVersion")}")
    implementation("com.badlogicgames.gdx:gdx-ai:${project.property("aiVersion")}")
    implementation("com.badlogicgames.box2dlights:box2dlights:${project.property("box2dlightsVersion")}")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:${project.property("gdxControllersVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.property("coroutines")}")
    api(project(":commons"))
}
