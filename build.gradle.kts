val vertxVersion = "4.5.3"
val log4jVersion = "2.21.1"

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("kapt") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    `maven-publish`
}

group = "com.panomc"
version =
    (if (project.hasProperty("version") && project.findProperty("version") != "unspecified") project.findProperty("version") else "local-build")!!

val buildType = project.findProperty("buildType") as String? ?: "alpha"
val timeStamp: String by project
val buildDir by extra { file("${rootProject.layout.buildDirectory.get()}/libs") }

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/iovertx-3720/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("io.vertx:vertx-unit:$vertxVersion")

    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-web-client:$vertxVersion")
    implementation("io.vertx:vertx-mysql-client:$vertxVersion")
    implementation("io.vertx:vertx-mail-client:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-web-templ-handlebars:$vertxVersion")
    implementation("io.vertx:vertx-config:$vertxVersion")
    implementation("io.vertx:vertx-config-hocon:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-web-validation:$vertxVersion")
    implementation("io.vertx:vertx-json-schema:$vertxVersion")
    implementation("io.vertx:vertx-web-proxy:$vertxVersion")

    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    implementation("com.auth0:java-jwt:4.4.0")


    implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = log4jVersion)
    implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = log4jVersion)
    implementation(group = "org.apache.logging.log4j", name = "log4j-slf4j2-impl", version = log4jVersion)

    // recaptcha v2 1.0.4
    implementation("com.github.triologygmbh:reCAPTCHA-V2-java:1.0.4")

    // https://mvnrepository.com/artifact/commons-codec/commons-codec
    implementation(group = "commons-codec", name = "commons-codec", version = "1.16.1")

    // https://mvnrepository.com/artifact/org.springframework/spring-context
    implementation("org.springframework:spring-context:5.3.32")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.named("jar").configure {
    enabled = false
}

tasks {
    register("copyJar") {
        dependsOn(shadowJar)

        doLast {
            if (shadowJar.get().archiveFile.get().asFile.parentFile.absolutePath != buildDir.absolutePath) {
                copy {
                    from(shadowJar.get().archiveFile.get().asFile.absolutePath)
                    into(buildDir)
                }
            }
        }
    }

    build {
        dependsOn("copyJar")
    }

    register("buildDev") {
        dependsOn("build")
    }

    shadowJar {
        dependsOn(distTar, distZip)

        manifest {
            val attrMap = mutableMapOf<String, String>()

            if (project.gradle.startParameter.taskNames.contains("buildDev"))
                attrMap["MODE"] = "DEVELOPMENT"

            attrMap["VERSION"] = version.toString()
            attrMap["BUILD_TYPE"] = buildType

            attributes(attrMap)
        }

        archiveFileName.set("${rootProject.name}-${version}.jar")

        if (project.gradle.startParameter.taskNames.contains("publish")) {
            archiveFileName.set(archiveFileName.get().lowercase())
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<JavaExec>("run") {
    environment("EnvironmentType", "DEVELOPMENT")
    environment("PanoVersion", version)
    environment("PanoBuildType", buildType)
}

application {
    mainClass.set("com.panomc.platform.Main")
}

publishing {
    repositories {
        maven {
            name = "Pano"
            url = uri("https://maven.pkg.github.com/panocms/pano")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME_GITHUB")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN_GITHUB")
            }
        }
    }

    publications {
        create<MavenPublication>("shadow") {
            project.extensions.configure<com.github.jengelman.gradle.plugins.shadow.ShadowExtension> {
                artifactId = "core"
                component(this@create)
            }
        }
    }
}