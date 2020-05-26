package com.panomc.platform.route.api.get.panel

import com.panomc.platform.ErrorCode
import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.model.*
import com.panomc.platform.util.*
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject

class BasicDataAPI : PanelApi() {
    override val routeType = RouteType.GET

    override val routes = arrayListOf("/api/panel/basicData")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var platformCodeManager: PlatformCodeManager

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        databaseManager.createConnection { connection, _ ->
            if (connection == null)
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))
            else {
                val token = context.getCookie("pano_token").value

                getUserIDFromToken(connection, token, handler) { userID ->
                    getBasicUserData(connection, userID, handler) { getBasicUserData ->
                        getNotificationsCount(connection, userID, handler) { count ->
                            databaseManager.closeConnection(connection) {
                                handler.invoke(
                                    Successful(
                                        mapOf(
                                            "user" to getBasicUserData,
                                            "website" to mapOf(
                                                "name" to configManager.config["website-name"],
                                                "description" to configManager.config["website-description"]
                                            ),
                                            "platform_server_match_key" to platformCodeManager.getPlatformKey(),
                                            "platform_server_match_key_time_started" to platformCodeManager.getTimeStarted(),
                                            "platform_host_address" to context.request().host(),
                                            "servers" to listOf<Map<String, Any?>>(),
                                            "notifications_count" to count
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getUserIDFromToken(
        connection: Connection,
        token: String,
        resultHandler: (result: Result) -> Unit,
        handler: (userID: Int) -> Unit
    ) {
        val query =
            "SELECT `user_id` FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}token where `token` = ?"

        databaseManager.getSQLConnection(connection).queryWithParams(query, JsonArray().add(token)) { queryResult ->
            if (queryResult.succeeded())
                handler.invoke(queryResult.result().results[0].getInteger(0))
            else
                databaseManager.closeConnection(connection) {
                    resultHandler.invoke(Error(ErrorCode.PANEL_BASIC_DATA_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_8))
                }
        }
    }

    private fun getBasicUserData(
        connection: Connection,
        userID: Int,
        resultHandler: (result: Result) -> Unit,
        handler: (data: Map<String, Any>) -> Unit
    ) {
        val query =
            "SELECT `username`, `email`, `permission_id` FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}user where `id` = ?"

        databaseManager.getSQLConnection(connection).queryWithParams(
            query,
            JsonArray().add(userID)
        ) { queryResult ->
            if (queryResult.succeeded())
                handler.invoke(
                    mapOf(
                        "username" to queryResult.result().results[0].getString(0),
                        "email" to queryResult.result().results[0].getString(1),
                        "permission_id" to queryResult.result().results[0].getInteger(2)
                    )
                )
            else
                databaseManager.closeConnection(connection) {
                    resultHandler.invoke(Error(ErrorCode.PANEL_BASIC_DATA_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_7))
                }
        }
    }

    private fun getNotificationsCount(
        connection: Connection,
        userID: Int,
        resultHandler: (result: Result) -> Unit,
        handler: (count: Int) -> Unit
    ) {
        val query =
            "SELECT count(`id`) FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}panel_notification WHERE (`user_id` = ? OR `user_id` = ?) AND `status` = ? ORDER BY `date` DESC, `id` DESC"

        databaseManager.getSQLConnection(connection)
            .queryWithParams(query, JsonArray().add(userID).add(-1).add(NotificationStatus.NOT_READ)) { queryResult ->
                if (queryResult.succeeded())
                    handler.invoke(queryResult.result().results[0].getInteger(0))
                else
                    databaseManager.closeConnection(connection) {
                        resultHandler.invoke(Error(ErrorCode.PANEL_BASIC_DATA_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_68))
                    }
            }
    }
}