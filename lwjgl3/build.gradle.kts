plugins {
  application
}

application {
  mainClass.set("com.github.quillraven.quillycrawler.lwjgl3.LauncherKt")

  version = "0.0.1-SNAPSHOT"
  applicationName = "Quilly Crawler"
}

sourceSets {
  main {
    resources.srcDir(rootProject.file("assets"))
  }
}

dependencies {
  implementation(projects.core)
  implementation(libs.gdx.backend)
  implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdxVersion.get()}:natives-desktop")
  implementation("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdxVersion.get()}:natives-desktop")
  implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:${libs.versions.gdxControllersVersion.get()}")
}

tasks {
  jar {
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveFileName.set("${application.applicationName}-${project.version}.jar")

    manifest {
      attributes["Main-Class"] = application.mainClass.get()
    }
  }
}

