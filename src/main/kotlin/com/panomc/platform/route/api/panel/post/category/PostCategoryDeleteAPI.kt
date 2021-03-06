package com.panomc.platform.route.api.panel.post.category

import com.panomc.platform.ErrorCode
import com.panomc.platform.model.*
import io.vertx.core.AsyncResult
import io.vertx.ext.web.RoutingContext
import io.vertx.sqlclient.SqlConnection

class PostCategoryDeleteAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/post/category/delete")

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson
        val id = data.getInteger("id")

        databaseManager.createConnection((this::createConnectionHandler)(handler, id))
    }

    private fun createConnectionHandler(handler: (result: Result) -> Unit, id: Int) =
        handler@{ sqlConnection: SqlConnection?, _: AsyncResult<SqlConnection> ->
            if (sqlConnection == null) {
                handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))

                return@handler
            }

            databaseManager.getDatabase().postCategoryDao.isExistsByID(
                id,
                sqlConnection,
                (this::isExistsByID)(handler, sqlConnection, id)
            )
        }

    private fun isExistsByID(handler: (result: Result) -> Unit, sqlConnection: SqlConnection, id: Int) =
        handler@{ exists: Boolean?, _: AsyncResult<*> ->

            if (exists == null) {
                databaseManager.closeConnection(sqlConnection) {
                    handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_98))
                }
                return@handler
            }

            if (!exists) {
                databaseManager.closeConnection(sqlConnection) {
                    handler.invoke(Error(ErrorCode.NOT_EXISTS))
                }
            }

            databaseManager.getDatabase().postDao.removePostCategoriesByCategoryID(
                id,
                sqlConnection,
                (this::removePostCategoriesByCategoryIDHandler)(handler, sqlConnection, id)
            )
        }

    private fun removePostCategoriesByCategoryIDHandler(
        handler: (result: Result) -> Unit,
        sqlConnection: SqlConnection,
        id: Int
    ) = handler@{ result: Result?, _: AsyncResult<*> ->
        if (result == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_121))
            }

            return@handler
        }

        databaseManager.getDatabase().postCategoryDao.deleteByID(
            id,
            sqlConnection,
            (this::deleteByIDHandler)(handler, sqlConnection)
        )
    }

    private fun deleteByIDHandler(handler: (result: Result) -> Unit, sqlConnection: SqlConnection) =
        handler@{ result: Result?, _: AsyncResult<*> ->
            databaseManager.closeConnection(sqlConnection) {
                if (result == null) {
                    handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_97))

                    return@closeConnection
                }

                handler.invoke(Successful())
            }
        }
}