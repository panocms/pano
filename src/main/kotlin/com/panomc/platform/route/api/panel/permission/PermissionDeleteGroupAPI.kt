package com.panomc.platform.route.api.panel.permission

import com.panomc.platform.ErrorCode
import com.panomc.platform.db.model.PermissionGroup
import com.panomc.platform.model.*
import io.vertx.core.AsyncResult
import io.vertx.ext.web.RoutingContext
import io.vertx.sqlclient.SqlConnection

class PermissionDeleteGroupAPI : PanelApi() {
    override val routeType = RouteType.POST

    override val routes = arrayListOf("/api/panel/permission/delete/group")

    override fun getHandler(context: RoutingContext, handler: (result: Result) -> Unit) {
        val data = context.bodyAsJson

        val permissionGroupID = data.getInteger("id")

        databaseManager.createConnection(
            (this::createConnectionHandler)(
                handler,
                permissionGroupID
            )
        )
    }

    private fun createConnectionHandler(
        handler: (result: Result) -> Unit,
        permissionGroupID: Int
    ) = handler@{ sqlConnection: SqlConnection?, _: AsyncResult<SqlConnection> ->
        if (sqlConnection == null) {
            handler.invoke(Error(ErrorCode.CANT_CONNECT_DATABASE))

            return@handler
        }

        databaseManager.getDatabase().permissionGroupDao.isThereByID(
            permissionGroupID,
            sqlConnection,
            (this::isThereByIDHandler)(sqlConnection, handler, permissionGroupID)
        )
    }

    private fun isThereByIDHandler(
        sqlConnection: SqlConnection,
        handler: (result: Result) -> Unit,
        permissionGroupID: Int,
    ) = handler@{ isTherePermissionGroup: Boolean?, _: AsyncResult<*> ->
        if (isTherePermissionGroup == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_187))
            }

            return@handler
        }

        if (!isTherePermissionGroup) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.NOT_EXISTS))
            }

            return@handler
        }

        databaseManager.getDatabase().permissionGroupDao.getPermissionGroupByID(
            permissionGroupID,
            sqlConnection,
            (this::getPermissionGroupByIDHandler)(sqlConnection, handler, permissionGroupID)
        )
    }

    private fun getPermissionGroupByIDHandler(
        sqlConnection: SqlConnection,
        handler: (result: Result) -> Unit,
        permissionGroupID: Int,
    ) = handler@{ permissionGroup: PermissionGroup?, _: AsyncResult<*> ->
        if (permissionGroup == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_188))
            }

            return@handler
        }

        if (permissionGroup.name == "admin") {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_189))
            }

            return@handler
        }

        databaseManager.getDatabase().permissionGroupPermsDao.removePermissionGroup(
            permissionGroupID,
            sqlConnection,
            (this::removePermissionGroupHandler)(
                sqlConnection,
                handler,
                permissionGroupID
            )
        )
    }

    private fun removePermissionGroupHandler(
        sqlConnection: SqlConnection,
        handler: (result: Result) -> Unit,
        permissionGroupID: Int
    ) = handler@{ result: Result?, _: AsyncResult<*> ->
        if (result == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_190))
            }

            return@handler
        }

        databaseManager.getDatabase().userDao.removePermissionGroupByPermissionGroupID(
            permissionGroupID,
            sqlConnection,
            (this::removePermissionGroupByPermissionGroupIDHandler)(sqlConnection, handler, permissionGroupID)
        )
    }

    private fun removePermissionGroupByPermissionGroupIDHandler(
        sqlConnection: SqlConnection,
        handler: (result: Result) -> Unit,
        permissionGroupID: Int
    ) = handler@{ result: Result?, _: AsyncResult<*> ->
        if (result == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_191))
            }

            return@handler
        }

        databaseManager.getDatabase().permissionGroupDao.deleteByID(
            permissionGroupID,
            sqlConnection,
            (this::deleteByIDHandler)(sqlConnection, handler)
        )
    }

    private fun deleteByIDHandler(
        sqlConnection: SqlConnection,
        handler: (result: Result) -> Unit
    ) = handler@{ result: Result?, _: AsyncResult<*> ->
        databaseManager.closeConnection(sqlConnection) {
            if (result == null) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_192))

                return@closeConnection
            }

            handler.invoke(Successful())
        }
    }
}