package com.panomc.platform.config

import com.panomc.platform.annotation.Migration
import com.panomc.platform.util.KeyGeneratorUtil
import com.panomc.platform.util.UpdatePeriod
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import org.slf4j.Logger
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.util.*

@Lazy
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ConfigManager(
    vertx: Vertx,
    private val logger: Logger,
    applicationContext: AnnotationConfigApplicationContext
) {

    companion object {
        private const val CONFIG_VERSION = 1

        private val DEFAULT_CONFIG by lazy {
            val key = KeyGeneratorUtil.generateJWTKey()

            JsonObject(
                mapOf(
                    "config-version" to CONFIG_VERSION,
                    "development-mode" to true,
                    "locale" to "en-US",

                    "website-name" to "",
                    "website-description" to "",
                    "support-email" to "",
                    "server-ip-address" to "play.ipadress.com",
                    "server-game-version" to "1.8.x",
                    "keywords" to listOf<String>(),

                    "setup" to mapOf(
                        "step" to 0
                    ),

                    "database" to mapOf(
                        "host" to "",
                        "name" to "",
                        "username" to "",
                        "password" to "",
                        "prefix" to "pano_"
                    ),

                    "pano-account" to mapOf(
                        "username" to "",
                        "email" to "",
                        "access-token" to ""
                    ),

                    "current-theme" to "Vanilla",

                    "email" to mapOf(
                        "address" to "",
                        "host" to "",
                        "port" to 465,
                        "username" to "",
                        "password" to "",
                        "SSL" to true,
                        "TLS" to true,
                        "auth-method" to ""
                    ),

                    "jwt-key" to Base64.getEncoder().encode(key.toByteArray()),

                    "update-period" to UpdatePeriod.ONCE_PER_DAY.period,

                    "ui-address" to "http://localhost:3000",
                    "file-uploads-folder" to "file-uploads",
                    "file-paths" to mapOf<String, String>()
                )
            )
        }

        fun JsonObject.putAll(jsonObject: Map<String, Any>) {
            jsonObject.forEach {
                this.put(it.key, it.value)
            }
        }
    }

    private val configFilePath by lazy {
        System.getProperty("pano.configFile", "config.conf")
    }

    fun saveConfig(config: JsonObject = this.config) {
        val renderOptions = ConfigRenderOptions
            .defaults()
            .setJson(false)           // false: HOCON, true: JSON
            .setOriginComments(false) // true: add comment showing the origin of a value
            .setComments(true)        // true: keep original comment
            .setFormatted(true)

        val parsedConfig = ConfigFactory.parseString(config.toString())

        if (configFile.parentFile != null && !configFile.parentFile.exists()) {
            configFile.parentFile.mkdirs()
        }

        configFile.writeText(parsedConfig.root().render(renderOptions))
    }

    fun getConfig() = config

    internal suspend fun init() {
        if (!configFile.exists()) {
            saveConfig(DEFAULT_CONFIG)
        }

        val configValues: Map<String, Any>

        try {
            configValues = configRetriever.config.await().map
        } catch (e: Exception) {
            logger.error("Error occurred while loading config file! Error: $e")
            logger.info("Using default config!")

            config.putAll(DEFAULT_CONFIG.map)

            return
        }

        config.putAll(configValues)

        logger.info("Checking available config migrations")

        migrate()

        listenConfigFile()
    }

    private fun getConfigVersion(): Int = config.getInteger("config-version")

    private val config = JsonObject()

    private val migrations by lazy {
        val beans = applicationContext.getBeansWithAnnotation(Migration::class.java)

        beans.filter { it.value is ConfigMigration }.map { it.value as ConfigMigration }.sortedBy { it.FROM_VERSION }
    }

    private val configFile = File(configFilePath)

    private val fileStore = ConfigStoreOptions()
        .setType("file")
        .setFormat("hocon")
        .setConfig(JsonObject().put("path", configFilePath))

    private val options = ConfigRetrieverOptions().addStore(fileStore)

    private val configRetriever = ConfigRetriever.create(vertx, options)

    private fun migrate(configVersion: Int = getConfigVersion(), saveConfig: Boolean = true) {
        migrations
            .find { configMigration -> configMigration.isMigratable(configVersion) }
            ?.let { migration ->
                logger.info("Migration Found! Migrating config from version ${migration.FROM_VERSION} to ${migration.VERSION}: ${migration.VERSION_INFO}")

                config.put("config-version", migration.VERSION)

                migration.migrate(this)

                migrate(migration.VERSION, false)
            }

        if (saveConfig) {
            saveConfig()
        }
    }

    private fun listenConfigFile() {
        configRetriever.listen { change ->
            config.clear()

            updateConfig(change.newConfiguration)
        }
    }

    private fun updateConfig(newConfig: JsonObject) {
        newConfig.map.forEach {
            config.put(it.key, it.value)
        }
    }
}