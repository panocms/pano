package com.panomc.platform.route.api.panel.post

import com.panomc.platform.ErrorCode
import com.panomc.platform.db.model.Post
import com.panomc.platform.model.*
import com.panomc.platform.util.LoginUtil
import io.vertx.core.AsyncResult
import io.vertx.ext.web.RoutingContext
import io.vertx.sqlclient.SqlConnection

class PostPublishAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/post/publish")

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson
        val id = data.getValue("id").toString().toInt()
        val title = data.getString("title")
        val categoryID = data.getValue("category").toString().toInt()
        val text = data.getString("text")
        val imageCode = data.getString("imageCode") ?: ""

        val idOrToken = LoginUtil.getUserIDOrToken(context)

        if (idOrToken == null || (idOrToken !is Int && idOrToken !is String)) {
            handler.invoke(Error(ErrorCode.NOT_LOGGED_IN))

            return
        }

        databaseManager.createConnection(
            (this::createConnectionHandler)(
                handler,
                id,
                title,
                categoryID,
                text,
                imageCode,
                idOrToken
            )
        )
    }

    private fun createConnectionHandler(
        handler: (result: Result) -> Unit,
        id: Int,
        title: String,
        categoryID: Int,
        text: String,
        imageCode: String,
        idOrToken: Any
    ) = handler@{ sqlConnection: SqlConnection?, _: AsyncResult<SqlConnection> ->
        if (sqlConnection == null) {
            handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))

            return@handler
        }

        if (idOrToken is Int) {
            updateOrInsert(handler, sqlConnection, id, title, categoryID, text, imageCode, idOrToken)

            return@handler
        }

        databaseManager.getDatabase().tokenDao.getUserIDFromToken(
            idOrToken as String,
            sqlConnection,
            (this::getUserIDFromTokenHandler)(handler, sqlConnection, id, title, categoryID, text, imageCode)
        )
    }

    private fun getUserIDFromTokenHandler(
        handler: (result: Result) -> Unit,
        sqlConnection: SqlConnection,
        id: Int,
        title: String,
        categoryID: Int,
        text: String,
        imageCode: String,
    ) = handler@{ userID: Int?, _: AsyncResult<*> ->
        if (userID == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(
                    Error(
                        ErrorCode.UNKNOWN_ERROR_116
                    )
                )
            }

            return@handler
        }

        updateOrInsert(handler, sqlConnection, id, title, categoryID, text, imageCode, userID)
    }

    private fun updateOrInsert(
        handler: (result: Result) -> Unit,
        sqlConnection: SqlConnection,
        id: Int,
        title: String,
        categoryID: Int,
        text: String,
        imageCode: String,
        userID: Int
    ) {
        val post = Post(
            id,
            title,
            categoryID,
            userID,
            text,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            1,
            imageCode,
            0
        )

        if (id == -1) {
            databaseManager.getDatabase().postDao.insertAndPublish(
                post,
                sqlConnection,
                (this::insertAndPublishHandler)(handler, sqlConnection)
            )

            return
        }

        databaseManager.getDatabase().postDao.updateAndPublish(
            userID,
            post,
            sqlConnection,
            (this::updateAndPublishHandler)(handler, sqlConnection)
        )
    }

    private fun insertAndPublishHandler(
        handler: (result: Result) -> Unit,
        sqlConnection: SqlConnection
    ) = handler@{ postID: Long?, _: AsyncResult<*> ->
        databaseManager.closeConnection(sqlConnection) {
            if (postID == null) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_114))

                return@closeConnection
            }

            handler.invoke(
                Successful(
                    mapOf(
                        "id" to postID
                    )
                )
            )
        }
    }

    private fun updateAndPublishHandler(
        handler: (result: Result) -> Unit,
        sqlConnection: SqlConnection
    ) = handler@{ result: Result?, _: AsyncResult<*> ->
        databaseManager.closeConnection(sqlConnection) {
            if (result == null) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_115))

                return@closeConnection
            }

            handler.invoke(Successful())
        }
    }
}