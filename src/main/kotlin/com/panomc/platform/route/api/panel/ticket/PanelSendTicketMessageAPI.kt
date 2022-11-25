package com.panomc.platform.route.api.panel.ticket

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.TicketMessage
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class PanelSendTicketMessageAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager,
    setupManager: SetupManager
) : PanelApi(setupManager, authProvider) {
    override val paths = listOf(Path("/api/panel/tickets/:id/message", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .pathParameter(param("id", numberSchema()))
            .body(
                json(
                    objectSchema()
                        .property("message", stringSchema())
                )
            )
            .build()

    override suspend fun handler(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val ticketId = parameters.pathParameter("id").long
        val message = data.getString("message")

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlConnection = createConnection(databaseManager, context)

        val exists = databaseManager.ticketDao.isExistsById(ticketId, sqlConnection)

        if (!exists) {
            throw Error(ErrorCode.NOT_EXISTS)
        }

        val username = databaseManager.userDao.getUsernameFromUserId(userId, sqlConnection)

        val ticketMessage = TicketMessage(userId = userId, ticketId = ticketId, message = message, panel = 1)

        val messageId = databaseManager.ticketMessageDao.addMessage(ticketMessage, sqlConnection)

        databaseManager.ticketDao.makeStatus(ticketMessage.ticketId, 2, sqlConnection)

        databaseManager.ticketDao.updateLastUpdateDate(
            ticketMessage.ticketId,
            System.currentTimeMillis(),
            sqlConnection
        )

        return Successful(
            mapOf(
                "message" to mapOf(
                    "id" to messageId,
                    "userID" to ticketMessage.userId,
                    "ticketID" to ticketMessage.ticketId,
                    "username" to username,
                    "message" to ticketMessage.message,
                    "date" to ticketMessage.date,
                    "panel" to ticketMessage.panel
                )
            )
        )
    }
}