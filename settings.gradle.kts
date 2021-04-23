dependencyResolutionManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}

include("commons", "core", "lwjgl3")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
