package com.panomc.platform.route.api.post.panel.ticket

import com.panomc.platform.ErrorCode
import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.model.*
import com.panomc.platform.util.ConfigManager
import com.panomc.platform.util.Connection
import com.panomc.platform.util.DatabaseManager
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject
import kotlin.math.ceil

class TicketCategoryPageInitAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/initPage/tickets/categoryPage")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var configManager: ConfigManager

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson
        val page = data.getInteger("page")

        databaseManager.createConnection { connection, _ ->
            if (connection == null)
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))
            else
                getTicketCategoriesCount(connection, handler) { countOfTicketCategories ->
                    var totalPage = ceil(countOfTicketCategories.toDouble() / 10).toInt()

                    if (totalPage < 1)
                        totalPage = 1

                    if (page > totalPage || page < 1)
                        databaseManager.closeConnection(connection) {
                            handler.invoke(Error(ErrorCode.PAGE_NOT_FOUND))
                        }
                    else
                        getCategoriesByPage(connection, page, handler) { categories ->
                            val result = mutableMapOf<String, Any?>(
                                "categories" to categories,
                                "category_count" to countOfTicketCategories,
                                "total_page" to totalPage,
                                "host" to "http://"
                            )

                            databaseManager.closeConnection(connection) {
                                handler.invoke(Successful(result))
                            }
                        }
                }
        }
    }

    private fun getTicketCategoriesCount(
        connection: Connection,
        resultHandler: (result: Result) -> Unit,
        handler: (ticketCategoryCount: Int) -> Unit
    ) {
        val query =
            "SELECT COUNT(id) FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}ticket_category"

        databaseManager.getSQLConnection(connection).queryWithParams(query, JsonArray()) { queryResult ->
            if (queryResult.succeeded())
                handler.invoke(queryResult.result().results[0].getInteger(0))
            else
                databaseManager.closeConnection(connection) {
                    resultHandler.invoke(Error(ErrorCode.TICKET_CATEGORY_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_80))
                }
        }
    }

    private fun getCategoriesByPage(
        connection: Connection,
        page: Int,
        resultHandler: (result: Result) -> Unit,
        handler: (categories: List<Map<String, Any>>) -> Unit
    ) {
        val query =
            "SELECT id, title, description FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}ticket_category ORDER BY id DESC LIMIT 10 OFFSET ${(page - 1) * 10}"

        databaseManager.getSQLConnection(connection).queryWithParams(query, JsonArray()) { queryResult ->
            if (queryResult.succeeded()) {
                val categories = mutableListOf<Map<String, Any>>()

                queryResult.result().results.forEach {
                    categories.add(
                        mapOf(
                            "id" to it.getInteger(0),
                            "title" to it.getString(1),
                            "description" to it.getString(2)
                        )
                    )
                }

                handler.invoke(categories)
            } else
                databaseManager.closeConnection(connection) {
                    resultHandler.invoke(Error(ErrorCode.TICKET_CATEGORY_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_79))
                }
        }
    }
}