package com.panomc.platform.route.api.panel

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.SystemProperty
import com.panomc.platform.db.model.TicketCategory
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.json.schema.SchemaParser

@Endpoint
class PanelGetDashboardAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager,
    setupManager: SetupManager
) : PanelApi(setupManager, authProvider) {
    override val routeType = RouteType.GET

    override val routes = arrayListOf("/api/panel/dashboard")

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandler.builder(schemaParser).build()

    override suspend fun handler(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val result = mutableMapOf<String, Any?>(
            "gettingStartedBlocks" to mapOf(
                "welcomeBoard" to false
            ),
            "registeredPlayerCount" to 0,
            "postCount" to 0,
            "ticketCount" to 0,
            "openTicketCount" to 0,
            "tickets" to mutableListOf<Map<String, Any?>>()
        )

        val sqlConnection = createConnection(databaseManager, context)

        val isUserInstalled =
            databaseManager.systemPropertyDao.isUserInstalledSystemByUserId(userId, sqlConnection)

        if (isUserInstalled) {
            val systemProperty = databaseManager.systemPropertyDao.getValue(
                SystemProperty(
                    -1,
                    "show_getting_started",
                    ""
                ),
                sqlConnection
            ) ?: throw Error(ErrorCode.UNKNOWN)

            result["gettingStartedBlocks"] = mapOf(
                "welcomeBoard" to systemProperty.value.toBoolean()
            )
        }

        val userCount = databaseManager.userDao.count(
            sqlConnection
        )

        result["registeredPlayerCount"] = userCount

        val postCount = databaseManager.postDao.count(
            sqlConnection
        )

        result["postCount"] = postCount

        val ticketCount = databaseManager.ticketDao.count(
            sqlConnection
        )

        result["ticketCount"] = ticketCount

        if (ticketCount == 0) {
            return Successful(result)
        }

        val openTicketCount = databaseManager.ticketDao.countOfOpenTickets(sqlConnection)

        result["openTicketCount"] = openTicketCount

        val tickets = databaseManager.ticketDao.getLast5Tickets(sqlConnection)

        val userIdList = tickets.distinctBy { it.userId }.map { it.userId }

        val usernameList = databaseManager.userDao.getUsernameByListOfId(userIdList, sqlConnection)

        val categoryIdList = tickets.filter { it.categoryId != -1 }.distinctBy { it.categoryId }.map { it.categoryId }
        var ticketCategoryList: Map<Int, TicketCategory> = mapOf()

        if (categoryIdList.isNotEmpty()) {
            ticketCategoryList = databaseManager.ticketCategoryDao.getByIdList(categoryIdList, sqlConnection)
        }

        val ticketDataList = mutableListOf<Map<String, Any?>>()

        tickets.forEach { ticket ->
            ticketDataList.add(
                mapOf(
                    "id" to ticket.id,
                    "title" to ticket.title,
                    "category" to
                            if (ticket.categoryId == -1)
                                mapOf("id" to -1, "title" to "-")
                            else
                                ticketCategoryList.getOrDefault(
                                    ticket.categoryId,
                                    mapOf("id" to -1, "title" to "-")
                                ),
                    "writer" to mapOf(
                        "username" to usernameList[ticket.userId]
                    ),
                    "date" to ticket.date,
                    "lastUpdate" to ticket.lastUpdate,
                    "status" to ticket.status
                )
            )
        }

        result["tickets"] = ticketDataList

        return Successful(result)
    }
}