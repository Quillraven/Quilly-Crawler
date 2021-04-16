plugins {
  kotlin("jvm") version "1.4.32" apply false
}

subprojects {
  apply {
    plugin("org.jetbrains.kotlin.jvm")
  }

  val implementation by configurations

  dependencies {
    implementation(kotlin("stdlib"))
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.contracts.ExperimentalContracts"
    }
  }
}
