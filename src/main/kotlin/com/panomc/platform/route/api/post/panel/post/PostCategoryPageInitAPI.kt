package com.panomc.platform.route.api.post.panel.post

import com.panomc.platform.ErrorCode
import com.panomc.platform.Main.Companion.getComponent
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import io.vertx.ext.web.RoutingContext
import javax.inject.Inject
import kotlin.math.ceil

class PostCategoryPageInitAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/initPage/posts/categoryPage")

    init {
        getComponent().inject(this)
    }

    @Inject
    lateinit var databaseManager: DatabaseManager

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson
        val page = data.getInteger("page")

        databaseManager.createConnection { connection, _ ->
            if (connection == null) {
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))

                return@createConnection
            }

            databaseManager.getDatabase().postCategoryDao.getCount(databaseManager.getSQLConnection(connection)) { count, _ ->
                if (count == null)
                    databaseManager.closeConnection(connection) {
                        handler.invoke(Error(ErrorCode.POST_CATEGORY_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_86))
                    }
                else {
                    var totalPage = ceil(count.toDouble() / 10).toInt()

                    if (totalPage < 1)
                        totalPage = 1

                    if (page > totalPage || page < 1) {
                        databaseManager.closeConnection(connection) {
                            handler.invoke(Error(ErrorCode.PAGE_NOT_FOUND))
                        }

                        return@getCount
                    }

                    databaseManager.getDatabase().postCategoryDao.getCategories(
                        page,
                        databaseManager.getSQLConnection(connection)
                    ) { categories, _ ->
                        if (categories == null) {
                            databaseManager.closeConnection(connection) {
                                handler.invoke(Error(ErrorCode.POST_CATEGORY_PAGE_INIT_API_SORRY_AN_ERROR_OCCURRED_ERROR_CODE_85))
                            }

                            return@getCategories
                        }

                        val result = mutableMapOf<String, Any?>(
                            "categories" to categories,
                            "category_count" to count,
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
    }
}