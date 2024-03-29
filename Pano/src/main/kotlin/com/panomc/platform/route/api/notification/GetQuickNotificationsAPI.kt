package com.panomc.platform.route.api.notification

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

@Endpoint
class GetQuickNotificationsAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager
) : LoggedInApi() {
    override val paths = listOf(Path("/api/notifications/quick", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlClient = getSqlClient()

        val notifications = databaseManager.notificationDao.getLast5ByUserId(userId, sqlClient)

        val count = databaseManager.notificationDao.getCountOfNotReadByUserId(userId, sqlClient)

        val notificationsDataList = mutableListOf<Map<String, Any?>>()

        notifications.forEach { notification ->
            notificationsDataList.add(
                mapOf(
                    "id" to notification.id,
                    "type" to notification.type,
                    "properties" to notification.properties,
                    "date" to notification.date,
                    "status" to notification.status,
                    "isPersonal" to (notification.userId == userId)
                )
            )
        }

        return Successful(
            mutableMapOf<String, Any?>(
                "notifications" to notificationsDataList,
                "notificationCount" to count
            )
        )
    }
}