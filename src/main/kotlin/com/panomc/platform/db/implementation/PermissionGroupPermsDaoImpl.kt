package com.panomc.platform.db.implementation

import com.panomc.platform.annotation.Dao
import com.panomc.platform.db.DaoImpl
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.dao.PermissionGroupPermsDao
import com.panomc.platform.db.model.PermissionGroupPerms
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

@Dao
class PermissionGroupPermsDaoImpl(databaseManager: DatabaseManager) :
    DaoImpl(databaseManager, "permission_group_perms"), PermissionGroupPermsDao {
    override suspend fun init(sqlConnection: SqlConnection) {
        sqlConnection
            .query(
                """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `permission_id` int NOT NULL,
                              `permission_group_id` int NOT NULL,
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission Group Permission Table';
                        """
            )
            .execute()
            .await()
    }

    override suspend fun getPermissionGroupPerms(
        sqlConnection: SqlConnection
    ): List<PermissionGroupPerms> {
        val query =
            "SELECT `id`, `permission_id`, `permission_group_id` FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute()
            .await()

        val permissionGroupPerms = rows.map { row ->
            PermissionGroupPerms(row.getInteger(0), row.getInteger(1), row.getInteger(2))
        }

        return permissionGroupPerms
    }

    override suspend fun doesPermissionGroupHavePermission(
        permissionGroupID: Int,
        permissionID: Int,
        sqlConnection: SqlConnection
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` WHERE `permission_group_id` = ? AND  `permission_id` = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupID,
                    permissionID
                )
            ).await()

        return rows.toList()[0].getInteger(0) != 0
    }

    override suspend fun addPermission(
        permissionGroupID: Int,
        permissionID: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (`permission_id`, `permission_group_id`) VALUES (?, ?)"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionID,
                    permissionGroupID
                )
            ).await()
    }

    override suspend fun removePermission(
        permissionGroupID: Int,
        permissionID: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `permission_group_id` = ? AND `permission_id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupID,
                    permissionID
                )
            ).await()
    }

    override suspend fun removePermissionGroup(
        permissionGroupID: Int,
        sqlConnection: SqlConnection,
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `permission_group_id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupID
                )
            ).await()
    }
}