package com.panomc.platform.route.api.setup.step

import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.config.ConfigManager
import com.panomc.platform.model.Result
import com.panomc.platform.model.RouteType
import com.panomc.platform.model.SetupApi
import com.panomc.platform.model.Successful
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject

class NextStepAPI : SetupApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/setup/step/nextStep")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var configManager: ConfigManager

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson

        val clientStep = data.getInteger("step")

        if (clientStep == setupManager.getStep()) {
            var passStep = false

            if (clientStep == 0)
                passStep = true
            else if (clientStep == 1 && !data.getString("websiteName").isNullOrEmpty() && !data.getString("websiteDescription").isNullOrEmpty()) {
                configManager.getConfig()["website-name"] = data.getString("websiteName")
                configManager.getConfig()["website-description"] = data.getString("websiteDescription")

                passStep = true
            } else if (
                clientStep == 2 &&
                !data.getString("host").isNullOrEmpty() &&
                !data.getString("dbName").isNullOrEmpty() &&
                !data.getString("username").isNullOrEmpty()
            ) {
                @Suppress("UNCHECKED_CAST") val databaseOptions =
                    (configManager.getConfig()["database"] as MutableMap<String, Any>)

                databaseOptions.replace("host", data.getString("host"))
                databaseOptions.replace("name", data.getString("dbName"))
                databaseOptions.replace(
                    "username",
                    data.getString("username")
                )
                databaseOptions.replace(
                    "password",
                    if (data.getString("password").isNullOrEmpty()) "" else data.getString("password")
                )
                databaseOptions.replace(
                    "prefix",
                    if (data.getString("prefix").isNullOrEmpty()) "" else data.getString("prefix")
                )

                passStep = true
            }

            if (passStep)
                setupManager.nextStep()
        }

        handler.invoke(Successful(setupManager.getCurrentStepData()))
    }
}