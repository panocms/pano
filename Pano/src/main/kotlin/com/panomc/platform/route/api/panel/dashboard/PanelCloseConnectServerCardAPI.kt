package com.panomc.platform.route.api.panel.dashboard


import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.NoPermission
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

@Endpoint
class PanelCloseConnectServerCardAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/dashboard/closeConnectServerCard", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlClient = getSqlClient()

        val isUserInstalledSystem =
            databaseManager.systemPropertyDao.isUserInstalledSystemByUserId(userId, sqlClient)

        if (!isUserInstalledSystem) {
            throw NoPermission()
        }

        databaseManager.systemPropertyDao.update(
            "show_connect_server_info",
            "false",
            sqlClient
        )

        return Successful()
    }
}