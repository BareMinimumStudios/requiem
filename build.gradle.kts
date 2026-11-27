import net.msrandom.stubs.GenerateStubApi
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.cloche)
    alias(libs.plugins.mixinmcp.decompile)
}

group = providers.gradleProperty("maven_group").get()
version = providers.gradleProperty("mod_version").get()
val testIncapacitated = providers.gradleProperty("testIncapacitated").isPresent

repositories {
    mavenCentral()
    cloche {
        librariesMinecraft()
        main()
        mavenFabric()
        mavenNeoforgedMeta()
        mavenNeoforged()
        mavenParchment()
    }
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://maven.fzzyhmstrs.me/")
    if (testIncapacitated) {
        maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    }
}

cloche {
    metadata {
        modId = "requiem_cinematics"
        name = "Requiem"
        icon = "assets/requiem_cinematics/icon.png"
        description = "Cinematic death, resurrection, and configurable mortality."
        license = "BML-1.1"
        author("pokesmells")
        url = "https://github.com/BareMinimumStudios/requiem"
        dependencies {
            dependency {
                modId = "minecraft"
                version { start = "1.21.1"; end = "1.21.2"; endExclusive = true }
            }
        }
    }
    common {
        mixins.from("src/main/resources/requiem_cinematics.mixins.json")
        sourceSet.resources.exclude("requiem_cinematics.mixins.json")
        mappings {
            official()
            parchment(libs.versions.parchment)
        }
    }
    fabric("fabric:1.21.1") {
        minecraftVersion = "1.21.1"
        loaderVersion = libs.versions.fabric.loader
        includedClient()
        runs { server(); client() }
        dependencies {
            fabricApi(libs.versions.fabric.api)
            modImplementation(libs.fabric.language.kotlin)
            modImplementation(libs.fzzy.config.fabric)
            if (testIncapacitated) {
                modImplementation(libs.incapacitated.fabric)
                providers.gradleProperty("incapacitatedNestedMods").orNull?.let { path ->
                    modImplementation(fileTree(path) { include("cardinal-components-*.jar") })
                }
            }
        }
        metadata {
            dependencies {
                dependency { modId = "fabric-api"; version(libs.versions.fabric.api.get()) }
                dependency { modId = "fabric-language-kotlin"; version(libs.versions.fabric.language.kotlin.get()) }
                dependency { modId = "fzzy_config"; version(libs.versions.fzzy.fabric.get()) }
            }
            entrypoint("main") {
                adapter.set("kotlin")
                value.set("dev.pokesmells.requiem.FabricRequiem")
            }
            entrypoint("client") {
                adapter.set("kotlin")
                value.set("dev.pokesmells.requiem.FabricRequiemClient")
            }
        }
    }
    neoforge("neoforge:1.21.1") {
        minecraftVersion = "1.21.1"
        loaderVersion = libs.versions.neoforge.loader
        runs { server(); client() }
        dependencies {
            modImplementation(libs.neoforge.language.kotlin)
            modImplementation(libs.fzzy.config.neoforge)
            if (testIncapacitated) modImplementation(libs.incapacitated.neoforge)
        }
        metadata {
            modLoader = "kotlinforforge"
            loaderVersion { start = libs.versions.neoforge.language.kotlin.get() }
            dependencies {
                dependency { modId = "neoforge"; version(libs.versions.neoforge.loader.get()) }
                dependency { modId = "kotlinforforge"; version(libs.versions.neoforge.language.kotlin.get()) }
                dependency { modId = "fzzy_config"; version(libs.versions.fzzy.neoforge.get()) }
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("fabric1211") { kotlin.srcDirs("src/fabric/1.21.1/kotlin", "src/loaderShared/kotlin") }
    sourceSets.named("neoforge1211") { kotlin.srcDirs("src/neoforge/1.21.1/kotlin", "src/loaderShared/kotlin") }
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion.set("2.2.20")
    coreLibrariesVersion = "2.2.20"
}

tasks.matching { it.name.startsWith("accessWidenFabric1211") }.configureEach {
    dependsOn("generateFabric1211MappingsArtifact")
}
tasks.withType<GenerateStubApi>().configureEach {
    outputDirectory.set(layout.buildDirectory.dir("generated/$name"))
}

dependencies {
    testImplementation(kotlin("test-junit5", "2.2.20"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }



tasks.withType<JavaExec>().matching { it.name.startsWith("run") }.configureEach {
    workingDir = layout.buildDirectory.dir("run/$name").get().asFile
    doFirst { workingDir.mkdirs() }
}

tasks.withType<Jar>().matching { it.name == "fabric1211Jar" || it.name == "neoforge1211Jar" }.configureEach {
    from("LICENSE") { into("META-INF/licenses") }
}
