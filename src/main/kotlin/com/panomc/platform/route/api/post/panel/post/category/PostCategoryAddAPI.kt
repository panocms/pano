package com.panomc.platform.route.api.post.panel.post.category

import com.beust.klaxon.JsonObject
import com.panomc.platform.ErrorCode
import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject

class PostCategoryAddAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/post/category/add")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var databaseManager: DatabaseManager

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson
        val title = data.getString("title")
        val description = data.getString("description")
        val url = data.getString("url")
        val color = data.getString("color")

        if (color.length != 7) {
            context.response().end(
                JsonObject(
                    mapOf(
                        "result" to "error"
                    )
                ).toJsonString()
            )

            return
        }

        val errors = mutableMapOf<String, Boolean>()

        if (title.isEmpty() || title.length > 32)
            errors["title"] = true

        if (description.isEmpty())
            errors["description"] = true

        if (url.isEmpty() || url.length < 3 || url.length > 32 || !url.matches(Regex("^[a-zA-Z0-9]+\$")))
            errors["url"] = true

        if (errors.isNotEmpty()) {
            handler.invoke(
                Errors(
                    errors
                )
            )

            return
        }

        databaseManager.createConnection { connection, _ ->
            if (connection == null) {
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))

                return@createConnection
            }

            databaseManager.getDatabase().postCategoryDao.isExistsByURL(
                url,
                databaseManager.getSQLConnection(connection)
            ) { exists, _ ->
                when {
                    exists == null -> databaseManager.closeConnection(connection) {
                        handler.invoke(Error(ErrorCode.POST_CATEGORY_ADD_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_94))
                    }
                    exists -> {
                        errors["url"] = true

                        databaseManager.closeConnection(connection) {
                            handler.invoke(
                                Errors(
                                    errors
                                )
                            )
                        }
                    }
                    else -> databaseManager.getDatabase().postCategoryDao.add(
                        PostCategory(-1, title, description, url, color),
                        databaseManager.getSQLConnection(connection)
                    ) { id, _ ->
                        if (id == null)
                            databaseManager.closeConnection(connection) {
                                handler.invoke(Error(ErrorCode.POST_CATEGORY_ADD_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_93))
                            }
                        else
                            databaseManager.closeConnection(connection) {
                                handler.invoke(
                                    Successful(
                                        mapOf(
                                            "id" to id
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