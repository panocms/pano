package com.panomc.platform.route.api.ticket

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.Ticket
import com.panomc.platform.db.model.TicketCategory
import com.panomc.platform.db.model.TicketMessage
import com.panomc.platform.error.NoPermission
import com.panomc.platform.error.NotExists
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.numberSchema

@Endpoint
class GetTicketAPI(
    private val databaseManager: DatabaseManager,
    private val authProvider: AuthProvider
) : LoggedInApi() {
    override val paths = listOf(Path("/api/tickets/:id", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .pathParameter(param("id", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val id = parameters.pathParameter("id").long
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlClient = getSqlClient()

        val exists = databaseManager.ticketDao.existsById(id, sqlClient)

        if (!exists) {
            throw NotExists()
        }

        val isBelong = databaseManager.ticketDao.isIdBelongToUserId(id, userId, sqlClient)

        if (!isBelong) {
            throw NoPermission()
        }

        val ticket = databaseManager.ticketDao.getById(id, sqlClient)!!

        val username = databaseManager.userDao.getUsernameFromUserId(ticket.userId, sqlClient)!!

        val messages = databaseManager.ticketMessageDao.getByTicketId(id, sqlClient)

        val userIdList = mutableListOf<Long>()

        messages
            .filter { ticketMessage -> ticketMessage.userId != -1L }
            .forEach { message ->
                if (userIdList.indexOf(message.userId) == -1)
                    userIdList.add(message.userId)
            }

        val usernameList = databaseManager.userDao.getUsernameByListOfId(userIdList, sqlClient)

        val count = databaseManager.ticketMessageDao.getCountByTicketId(ticket.id, sqlClient)

        if (ticket.categoryId == -1L) {
            return getResult(ticket, usernameList, null, username, messages, count)
        }

        val ticketCategory = databaseManager.ticketCategoryDao.getById(ticket.categoryId, sqlClient)

        return getResult(ticket, usernameList, ticketCategory, username, messages, count)
    }

    private fun getResult(
        ticket: Ticket,
        usernameList: Map<Long, String>,
        ticketCategory: TicketCategory?,
        username: String,
        ticketMessages: List<TicketMessage>,
        messageCount: Long
    ): Result {
        val messages = mutableListOf<Map<String, Any?>>()

        ticketMessages.forEach { ticketMessage ->
            messages.add(
                0,
                mapOf(
                    "id" to ticketMessage.id,
                    "userId" to ticketMessage.userId,
                    "ticketId" to ticketMessage.ticketId,
                    "username" to (usernameList[ticketMessage.userId] ?: "-"),
                    "message" to ticketMessage.message,
                    "date" to ticketMessage.date,
                    "panel" to ticketMessage.panel
                )
            )
        }

        return Successful(
            mapOf(
                "ticket" to mapOf(
                    "id" to ticket.id,
                    "username" to username,
                    "title" to ticket.title,
                    "category" to
                            if (ticketCategory == null)
                                mapOf("title" to "-", "url" to "-")
                            else
                                mapOf("title" to ticketCategory.title, "url" to ticketCategory.url),
                    "messages" to messages,
                    "status" to ticket.status,
                    "date" to ticket.date,
                    "messageCount" to messageCount
                )
            )
        )
    }
}