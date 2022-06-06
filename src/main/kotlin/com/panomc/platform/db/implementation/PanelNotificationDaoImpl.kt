package com.panomc.platform.db.implementation

import com.panomc.platform.annotation.Dao
import com.panomc.platform.db.DaoImpl
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.dao.PanelNotificationDao
import com.panomc.platform.db.model.PanelNotification
import com.panomc.platform.util.NotificationStatus
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

@Dao
class PanelNotificationDaoImpl(databaseManager: DatabaseManager) : DaoImpl(databaseManager, "panel_notification"),
    PanelNotificationDao {

    override suspend fun init(sqlConnection: SqlConnection) {
        sqlConnection
            .query(
                """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `user_id` int NOT NULL,
                              `type_ID` varchar(255) NOT NULL,
                              `date` BIGINT(20) NOT NULL,
                              `status` varchar(255) NOT NULL,
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Panel Notification table.';
                        """
            )
            .execute()
            .await()
    }

    override suspend fun add(
        panelNotification: PanelNotification,
        sqlConnection: SqlConnection
    ) {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (user_id, type_ID, date, status) " +
                    "VALUES (?, ?, ?, ?)"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    panelNotification.userId,
                    panelNotification.typeId,
                    panelNotification.date,
                    panelNotification.status
                )
            ).await()
    }

    override suspend fun getCountOfNotReadByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ): Int {
        val query =
            "SELECT count(`id`) FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ? AND `status` = ? ORDER BY `date` DESC, `id` DESC"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId,
                    NotificationStatus.NOT_READ,
                )
            ).await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun getCountByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ): Int {
        val query =
            "SELECT count(`id`) FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ? ORDER BY `date` DESC, `id` DESC"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId
                )
            ).await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun getLast10ByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ): List<PanelNotification> {
        val query =
            "SELECT `id`, `user_id`, `type_ID`, `date`, `status` FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ? ORDER BY `date` DESC, `id` DESC LIMIT 10"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId
                )
            ).await()

        val notifications = mutableListOf<PanelNotification>()

        if (rows.size() > 0)
            rows.forEach { row ->
                notifications.add(
                    PanelNotification(
                        row.getInteger(0),
                        row.getInteger(1),
                        row.getString(2),
                        row.getLong(3),
                        NotificationStatus.valueOf(row.getString(4))
                    )
                )
            }

        return notifications
    }

    override suspend fun get10ByUserIdAndStartFromId(
        userId: Int,
        notificationId: Int,
        sqlConnection: SqlConnection
    ): List<PanelNotification> {
        val query =
            "SELECT `id`, `user_id`, `type_ID`, `date`, `status` FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ? AND id < ? ORDER BY `date` DESC, `id` DESC LIMIT 10"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId,
                    notificationId
                )
            ).await()

        val notifications = mutableListOf<PanelNotification>()

        if (rows.size() > 0)
            rows.forEach { row ->
                notifications.add(
                    PanelNotification(
                        row.getInteger(0),
                        row.getInteger(1),
                        row.getString(2),
                        row.getLong(3),
                        NotificationStatus.valueOf(row.getString(4))
                    )
                )
            }

        return notifications
    }

    override suspend fun markReadLast10(
        userId: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET status = ? WHERE `user_id` = ? ORDER BY `date` DESC, `id` DESC LIMIT 10"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    NotificationStatus.READ,
                    userId
                )
            ).await()
    }

    override suspend fun markReadLast10StartFromId(
        userId: Int,
        notificationId: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET status = ? WHERE `user_id` = ? AND id < ?  ORDER BY `date` DESC, `id` DESC LIMIT 10"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    NotificationStatus.READ,
                    userId,
                    notificationId
                )
            ).await()
    }

    override suspend fun getLast5ByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ): List<PanelNotification> {
        val query =
            "SELECT `id`, `user_id`, `type_ID`, `date`, `status` FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ? ORDER BY `date` DESC, `id` DESC LIMIT 5"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId
                )
            ).await()

        val notifications = mutableListOf<PanelNotification>()

        if (rows.size() > 0)
            rows.forEach { row ->
                notifications.add(
                    PanelNotification(
                        row.getInteger(0),
                        row.getInteger(1),
                        row.getString(2),
                        row.getLong(3),
                        NotificationStatus.valueOf(row.getString(4))
                    )
                )
            }

        return notifications
    }

    override suspend fun markReadLat5ByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET status = ? WHERE `user_id` = ? ORDER BY `date` DESC, `id` DESC LIMIT 5"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    NotificationStatus.READ,
                    userId
                )
            ).await()
    }

    override suspend fun existsById(
        id: Int,
        sqlConnection: SqlConnection
    ): Boolean {
        val query = "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()

        return rows.toList()[0].getInteger(0) == 1
    }

    override suspend fun getById(
        id: Int,
        sqlConnection: SqlConnection
    ): PanelNotification? {
        val query =
            "SELECT `id`, `user_id`, `type_ID`, `date`, `status` FROM `${getTablePrefix() + tableName}` WHERE `id` = ? ORDER BY `date` DESC, `id` DESC LIMIT 5"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        val notification = PanelNotification(
            row.getInteger(0),
            row.getInteger(1),
            row.getString(2),
            row.getLong(3),
            NotificationStatus.valueOf(row.getString(4))
        )

        return notification
    }

    override suspend fun deleteById(
        id: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).await()
    }

    override suspend fun deleteAllByUserId(
        userId: Int,
        sqlConnection: SqlConnection
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `user_id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    userId
                )
            )
            .await()
    }
}