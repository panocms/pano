package com.panomc.platform.route.api.profile

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

@Endpoint
class GetProfileAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager
) : LoggedInApi() {
    override val paths = listOf(Path("/api/profile", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val response = mutableMapOf<String, Any?>()

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlClient = getSqlClient()

        val user = databaseManager.userDao.getById(userId, sqlClient)!!

        response["registerDate"] = user.registerDate
        response["lastLoginDate"] = user.lastLoginDate

        return Successful(response)
    }
}