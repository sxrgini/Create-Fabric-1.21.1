// versions
// https://parchmentmc.org/docs/getting-started
val parchmentVersion = "2024.11.17"
// https://fabricmc.net/develop/
val minecraftVersion = "1.21.1"
val loaderVersion = "0.19.3"
val fapiVersion = "0.115.1+1.21.1"

// in-house dependencies
val flywheelVersion = "1.0.6-44"
val ponderVersion = "1.0.69"
val registrateVersion = "1.3.77-MC1.21.1"
val milkLibVersion = "1.2.60"
// https://modrinth.com/mod/porting_lib/versions - pick the newest 1.21.1 version and paste its version number here
val portingLibVersion = "3.1.0-beta.90+1.21.1"

// external dependencies
val configApiVersion = "21.1.3"
val nightConfigVersion =  "3.6.3"
val jsr305Version = "3.0.2"

// compat
// https://modrinth.com/mod/cc-tweaked/versions
val ccVersion = "1.115.1"
// for CC - https://modrinth.com/mod/cloth-config/versions
val clothVersion = "15.0.140+fabric"
// https://modrinth.com/mod/jei/versions
val jeiVersion = "19.21.0.247"
// https://modrinth.com/mod/rei/versions
val reiVersion = "16.0.799"
// https://modrinth.com/mod/emi/versions
val emiVersion = "1.1.20+1.21.1"
// https://modrinth.com/mod/botania
val botaniaVersion = "1.19.2-436-FABRIC"
// https://modrinth.com/mod/modmenu/versions
val modmenuVersion = "11.0.3"
// https://modrinth.com/mod/sandwichable/versions
val sandwichableVersion = "1.3.1+1.20.1"
// https://modrinth.com/mod/sodium
val sodiumVersion = "mc1.21.1-0.6.9-fabric"
// https://github.com/emilyploszaj/trinkets/releases/
val trinketsVersion = "3.10.0"
// for Trinkets - https://modrinth.com/mod/cardinal-components-api/versions
val ccaVersion = "6.1.2"
// https://modrinth.com/mod/journeymap
val jmVersion = "1.21.1-6.0.0-beta.39+fabric"
// check the jm jar, it's JiJ
val jmApiVersion = "1.20-1.9-SNAPSHOT"

// dev stuff
val ccRuntime = false
val recipeViewer = "jei" // jei, rei, or emi

plugins {
    id("fabric-loom") version "1.10.+"
    id("maven-publish")
}

val buildNum = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .filter(String::isNotEmpty)
    .map { "-build.$it" }
    .orElse("-local")
    .getOrElse("")

version = "6.0.0.0+mc$minecraftVersion$buildNum"

group = "com.simibubi.create"
base.archivesName = "create-fabric"

repositories {
    maven("https://maven.parchmentmc.org") // Parchment
    maven("https://maven.fabricmc.net") // FAPI, Loader
    maven("https://maven.createmod.net") // Ponder, Flywheel
    maven("https://mvn.devos.one/snapshots") // Registrate, Forge Tags, Milk Lib
    maven("https://mvn.devos.one/releases") // Porting Lib
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven") // Forge Config API Port
    maven("https://maven.shedaniel.me") // REI and deps
    maven("https://api.modrinth.com/maven") { // LazyDFU, Sodium, Sandwichable
        content { includeGroupAndSubgroups("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com") // Mod Menu, Trinkets
    maven("https://maven.squiddev.cc") // CC:T
    maven("https://modmaven.dev") // Botania
    maven("https://maven.jamieswhiteshirt.com/libs-release") { // Reach Entity Attributes
        content { includeGroup("com.jamieswhiteshirt") }
    }
    maven("https://maven.ladysnake.org/releases") // CCA, for Trinkets
    maven("https://maven.saps.dev/releases") // FTB
    maven("https://maven.architectury.dev") // Architectury API
    maven("https://jm.gserv.me/repository/maven-public/") // Journey map
}

val ponder = file("Ponder")

dependencies {
    // setup
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.layered {
        officialMojangMappings { nameSyntheticMembers = false }
        parchment("org.parchmentmc.data:parchment-$minecraftVersion:$parchmentVersion@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // dependencies
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fapiVersion")

    // Registrate pulls in Porting Lib beta.39 modules whose injected interfaces clash with beta.90
    // (fix found in the Blockfield fork). Porting Lib is declared explicitly below instead.
    modApi(include("com.tterrag.registrate_fabric:Registrate:$registrateVersion") {
        exclude(group = "io.github.fabricators_of_create.Porting-Lib")
    })

    modApi(include("com.electronwill.night-config:core:$nightConfigVersion")!!)
    modApi(include("com.electronwill.night-config:toml:$nightConfigVersion")!!)
    modApi(include("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:$configApiVersion")!!)
    modApi(include("dev.engine-room.flywheel:flywheel-fabric-$minecraftVersion:$flywheelVersion")!!)
    // Flywheel 1.0.6 splits out its API jar; the server needs it too (visuals are registered in AllBlocks)
    modApi(include("dev.engine-room.flywheel:flywheel-fabric-api-$minecraftVersion:$flywheelVersion")!!)
    // Milk Lib for 1.21.1 isn't on a maven; this is the 1.21.1 build from Modrinth (same jar the Blockfield fork uses).
    // Not bundled: install Milk Lib separately in the pack.
    modApi(files("libs/milk-lib-1.1.0-patch+1.21.1.jar"))

    // Porting Lib (module set matches the Blockfield fork). accessors and lazy_registration were
    // dropped from Porting Lib after beta.54, so those two stay on the older version.
    val portingLibModules = listOf(
        "attributes", "base", "blocks", "brewing", "chunk_loading", "client_events", "common", "config",
        "core", "data", "entity", "fluids", "gametest", "gui_utils", "item_abilities", "items",
        "level_events", "loot", "mixin_extensions", "model_loader", "models", "obj_loader",
        "recipe_book_categories", "render_types", "tags", "transfer", "registry", "resources", "model_data"
    )
    for (module in portingLibModules) {
        modApi(include("io.github.fabricators_of_create.Porting-Lib:$module:$portingLibVersion")!!)
    }
    for (module in listOf("accessors", "lazy_registration")) {
        modApi(include("io.github.fabricators_of_create.Porting-Lib:$module:3.1.0-beta.54+1.21.1")!!)
    }
    api(include("com.google.code.findbugs:jsr305:$jsr305Version")!!)

    if (ponder.exists()) {
        implementation("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") { isTransitive = false }
        implementation("net.createmod.ponder:Ponder-Common-$minecraftVersion:$ponderVersion")
    } else {
        modRuntimeOnly(include("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion")!!)
        modCompileOnly("net.createmod.ponder:Ponder-Fabric-$minecraftVersion:$ponderVersion") {
            exclude(group = "io.github.fabricators_of_create.Porting-Lib")
        }
    }

    // compat
    modCompileOnly("cc.tweaked:cc-tweaked-$minecraftVersion-fabric-api:$ccVersion")

    modCompileOnly("vazkii.botania:Botania:$botaniaVersion") { isTransitive = false }
    modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")
    modCompileOnly("maven.modrinth:sandwichable:$sandwichableVersion")
    modCompileOnly("maven.modrinth:sodium:$sodiumVersion")

    modCompileOnly("dev.emi:trinkets:$trinketsVersion")
    // for Trinkets
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modCompileOnly("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")

    // Architectury for REI (1.21.1 version). FTB compat is parked in disabled-compat/ftb for now.
    modCompileOnly("dev.architectury:architectury-fabric:13.0.11")

    modCompileOnly("maven.modrinth:journeymap:$jmVersion")
    modCompileOnly("info.journeymap:journeymap-api:$jmApiVersion")

    // EMI
    modCompileOnly("dev.emi:emi-fabric:$emiVersion:api") { isTransitive = false }
    // JEI
    modCompileOnly("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion") { isTransitive = false }
    // REI
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:$reiVersion")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:$reiVersion")

    when (recipeViewer) {
        "jei" -> modLocalRuntime("mezz.jei:jei-$minecraftVersion-fabric:$jeiVersion")
        "rei" -> modLocalRuntime("me.shedaniel:RoughlyEnoughItems-fabric:$reiVersion")
        "emi" -> modLocalRuntime("dev.emi:emi-fabric:$emiVersion")
    }

    // dev env
    modLocalRuntime("com.terraformersmc:modmenu:$modmenuVersion")
    modLocalRuntime("dev.emi:trinkets:$trinketsVersion") { isTransitive = false }
    // for Trinkets
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-base:$ccaVersion")
    modLocalRuntime("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:$ccaVersion")
    if (ccRuntime) {
        modLocalRuntime("cc.tweaked:cc-tweaked-$minecraftVersion-fabric:$ccVersion")
        modLocalRuntime("maven.modrinth:cloth-config:$clothVersion")
    }
    // have deprecated modules present at runtime only
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api-deprecated:$fapiVersion")
}

sourceSets.named("main") {
    resources {
        srcDir("src/generated/resources")
        exclude(".cache/")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/create.accesswidener")

    runs {
        register("datagen") {
            client()
            name("Data Generation")
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${file("src/generated/resources")}")
            vmArg("-Dfabric-api.datagen.modid=create")
        }

        register("gametestServer") {
            server()
            name("Headlesss GameTests")
            ideConfigGenerated(false) // this run is for CI
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory}/junit.xml")
            runDir("run/gametest")
        }

        named("server") {
            runDir("run/server")
        }

        configureEach {
            vmArg("-XX:+AllowEnhancedClassRedefinition")
            vmArg("-XX:+IgnoreUnrecognizedVMOptions")
            property("mixin.debug.export", "true")
        }
    }
}

configurations {
    // this avoids remapping ponder when it's local
    named("runtimeClasspath") {
        attributes {
            attribute(Attribute.of("create.marker", String::class.java), "h")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    exclude("**/*.bbmodel", "**/*.lnk")

    val properties: MutableMap<String, Any> = mutableMapOf(
        "version" to version,
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion,
        "fabric_version" to fapiVersion,
        "forge_config_version" to configApiVersion,
        "milk_lib_version" to milkLibVersion
    )

    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

java {
    withSourcesJar()
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("10000")
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = "create-fabric-$minecraftVersion"
            from(components["java"])
        }
    }

    repositories {
        maven("https://mvn.devos.one/releases") {
            name = "devOsReleases"
            credentials(PasswordCredentials::class)
        }

        maven("https://mvn.devos.one/snapshots") {
            name = "devOsSnapshots"
            credentials(PasswordCredentials::class)
        }
    }
}
