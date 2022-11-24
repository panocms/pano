package com.panomc.platform.route.api.auth

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.json.schema.SchemaParser

@Endpoint
class LogoutAPI(
    setupManager: SetupManager,
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager
) : LoggedInApi(setupManager, authProvider) {
    override val paths = listOf(Path("/api/auth/logout", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandler.builder(schemaParser).build()

    override suspend fun handler(context: RoutingContext): Result {
        val sqlConnection = createConnection(databaseManager, context)

        authProvider.logout(context, sqlConnection)

        return Successful()
    }
}