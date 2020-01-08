package com.panomc.platform.route.api.post.panel.post

import com.beust.klaxon.JsonObject
import com.panomc.platform.ErrorCode
import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.model.*
import com.panomc.platform.util.*
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.RoutingContext
import java.util.*
import javax.inject.Inject

class EditPostPageInitAPI : Api() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/initPage/editPost")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var setupManager: SetupManager

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var configManager: ConfigManager

    override fun getHandler() = Handler<RoutingContext> { context ->
        if (!setupManager.isSetupDone()) {
            context.reroute("/")

            return@Handler
        }

        val auth = Auth()

        auth.isAdmin(context) { isAdmin ->
            if (isAdmin) {
                val response = context.response()

                response
                    .putHeader("content-type", "application/json; charset=utf-8")

                getEditPostPageData(context) { result ->
                    if (result is Successful) {
                        val responseMap = mutableMapOf<String, Any?>(
                            "result" to "ok"
                        )

                        responseMap.putAll(result.map)

                        response.end(
                            JsonObject(
                                responseMap
                            ).toJsonString()
                        )
                    } else if (result is Error)
                        response.end(
                            JsonObject(
                                mapOf(
                                    "result" to "error",
                                    "error" to result.errorCode
                                )
                            ).toJsonString()
                        )
                }
            } else
                context.reroute("/")
        }
    }

    private fun getEditPostPageData(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson

        val id = data.getInteger("id")

        databaseManager.createConnection { connection, _ ->
            if (connection == null)
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))
            else
                isPostExistsByID(connection, id, handler) { exists ->
                    if (!exists)
                        databaseManager.closeConnection(connection) {
                            handler.invoke(Error(ErrorCode.POST_NOT_FOUND))
                        }
                    else
                        getPost(connection, id, handler) { post ->
                            databaseManager.closeConnection(connection) {
                                handler.invoke(
                                    Successful(
                                        mapOf(
                                            "post" to post
                                        )
                                    )
                                )
                            }
                        }
                }
        }
    }

    private fun isPostExistsByID(
        connection: Connection,
        id: Int,
        resultHandler: (result: Result) -> Unit,
        handler: (exists: Boolean) -> Unit
    ) {
        val query =
            "SELECT COUNT(id) FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}post WHERE id = ?"

        databaseManager.getSQLConnection(connection)
            .queryWithParams(query, JsonArray().add(id)) { queryResult ->
                if (queryResult.succeeded())
                    handler.invoke(queryResult.result().results[0].getInteger(0) == 1)
                else
                    databaseManager.closeConnection(connection) {
                        resultHandler.invoke(Error(ErrorCode.EDIT_POST_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_55))
                    }
            }
    }

    private fun getPost(
        connection: Connection,
        id: Int,
        resultHandler: (result: Result) -> Unit,
        handler: (post: Map<String, Any>) -> Unit
    ) {
        val query =
            "SELECT id, title, category_id, writer_user_id, post, date, status, image FROM ${(configManager.config["database"] as Map<*, *>)["prefix"].toString()}post WHERE id = ?"

        databaseManager.getSQLConnection(connection).queryWithParams(query, JsonArray().add(id)) { queryResult ->
            if (queryResult.succeeded()) {

                val post = mapOf(
                    "id" to queryResult.result().results[0].getInteger(0),
                    "title" to queryResult.result().results[0].getString(1),
                    "category_id" to queryResult.result().results[0].getString(2),
                    "writer_user_id" to queryResult.result().results[0].getInteger(3),
                    "post" to String(Base64.getDecoder().decode(queryResult.result().results[0].getString(4))),
                    "date" to queryResult.result().results[0].getInteger(5),
                    "status" to queryResult.result().results[0].getInteger(6),
                    "image" to queryResult.result().results[0].getString(7)
                )

                handler.invoke(post)
            } else
                databaseManager.closeConnection(connection) {
                    resultHandler.invoke(Error(ErrorCode.EDIT_POST_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_54))
                }
        }
    }
}