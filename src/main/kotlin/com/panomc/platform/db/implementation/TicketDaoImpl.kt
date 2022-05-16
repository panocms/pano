package com.panomc.platform.db.implementation

import com.panomc.platform.annotation.Dao
import com.panomc.platform.db.DaoImpl
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.dao.TicketDao
import com.panomc.platform.db.model.Ticket
import io.vertx.core.json.JsonArray
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

@Dao
class TicketDaoImpl(databaseManager: DatabaseManager) : DaoImpl(databaseManager, "ticket"), TicketDao {

    override suspend fun init(sqlConnection: SqlConnection) {
        sqlConnection
            .query(
                """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `title` MEDIUMTEXT NOT NULL,
                              `category_id` int(11) NOT NULL,
                              `user_id` int(11) NOT NULL,
                              `date` BIGINT(20) NOT NULL,
                              `last_update` BIGINT(20) NOT NULL,
                              `status` int(1) NOT NULL,
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tickets table.';
                        """
            )
            .execute()
            .await()
    }

    override suspend fun count(sqlConnection: SqlConnection): Int {
        val query = "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute()
            .await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun countOfOpenTickets(
        sqlConnection: SqlConnection
    ): Int {
        val query = "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` WHERE status = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(1))
            .await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun getLast5Tickets(
        sqlConnection: SqlConnection
    ): List<Ticket> {
        val query =
            "SELECT id, title, category_id, user_id, `date`, `last_update`, status FROM `${getTablePrefix() + tableName}` ORDER BY `last_update` DESC, `id` DESC LIMIT 5"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute()
            .await()

        val tickets = mutableListOf<Ticket>()

        rows.forEach { row ->
            tickets.add(
                Ticket(
                    row.getInteger(0),
                    row.getString(1),
                    row.getInteger(2),
                    row.getInteger(3),
                    row.getLong(4),
                    row.getLong(5),
                    row.getInteger(6)
                )
            )
        }

        return tickets
    }

    override suspend fun getAllByPageAndPageType(
        page: Int,
        pageType: Int,
        sqlConnection: SqlConnection
    ): List<Ticket> {
        val query =
            "SELECT id, title, category_id, user_id, `date`, `last_update`, status FROM `${getTablePrefix() + tableName}` ${if (pageType != 2) "WHERE status = ? " else ""}ORDER BY ${if (pageType == 2) "`status` ASC, " else ""}`last_update` DESC, `id` DESC LIMIT 10 ${if (page == 1) "" else "OFFSET ${(page - 1) * 10}"}"

        val parameters = Tuple.tuple()

        if (pageType != 2)
            parameters.addInteger(pageType)

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()

        val tickets = mutableListOf<Ticket>()

        rows.forEach { row ->
            tickets.add(
                Ticket(
                    row.getInteger(0),
                    row.getString(1),
                    row.getInteger(2),
                    row.getInteger(3),
                    row.getLong(4),
                    row.getLong(5),
                    row.getInteger(6)
                )
            )
        }

        return tickets
    }

    override suspend fun getAllByPageAndCategoryID(
        page: Int,
        categoryID: Int,
        sqlConnection: SqlConnection
    ): List<Ticket> {
        val query =
            "SELECT `id`, `title`, `category_id`, `user_id`, `date`, `last_update`, `status` FROM `${getTablePrefix() + tableName}` WHERE `category_id` = ? ORDER BY `status`, `last_update` DESC, `id` DESC LIMIT 10 ${if (page == 1) "" else "OFFSET ${(page - 1) * 10}"}"

        val parameters = Tuple.tuple()

        parameters.addInteger(categoryID)

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()

        val tickets = mutableListOf<Ticket>()

        rows.forEach { row ->
            tickets.add(
                Ticket(
                    row.getInteger(0),
                    row.getString(1),
                    row.getInteger(2),
                    row.getInteger(3),
                    row.getLong(4),
                    row.getLong(5),
                    row.getInteger(6)
                )
            )
        }

        return tickets
    }

    override suspend fun getAllByUserIDAndPage(
        userID: Int,
        page: Int,
        sqlConnection: SqlConnection
    ): List<Ticket> {
        val query =
            "SELECT id, title, category_id, user_id, `date`, `last_update`, status FROM `${getTablePrefix() + tableName}` WHERE user_id = ? ORDER BY `last_update` DESC, `id` DESC LIMIT 10 ${if (page == 1) "" else "OFFSET ${(page - 1) * 10}"}"

        val parameters = Tuple.tuple()

        parameters.addInteger(userID)

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()

        val tickets = mutableListOf<Ticket>()

        rows.forEach { row ->
            tickets.add(
                Ticket(
                    row.getInteger(0),
                    row.getString(1),
                    row.getInteger(2),
                    row.getInteger(3),
                    row.getLong(4),
                    row.getLong(5),
                    row.getInteger(6)
                )
            )
        }

        return tickets
    }

    override suspend fun getCountByPageType(
        pageType: Int,
        sqlConnection: SqlConnection
    ): Int {
        val query =
            "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` ${if (pageType != 2) "WHERE status = ?" else ""}"

        val parameters = Tuple.tuple()

        if (pageType != 2)
            parameters.addInteger(pageType)

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun getByCategory(
        id: Int,
        sqlConnection: SqlConnection
    ): List<Ticket> {
        val query =
            "SELECT id, title, category_id, user_id, `date`, `last_update`, status FROM `${getTablePrefix() + tableName}` WHERE category_id = ? ORDER BY `last_update` DESC, `id` DESC LIMIT 5"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()

        val posts = mutableListOf<Ticket>()

        rows.forEach { row ->
            posts.add(
                Ticket(
                    row.getInteger(0),
                    row.getString(1),
                    row.getInteger(2),
                    row.getInteger(3),
                    row.getLong(4),
                    row.getLong(5),
                    row.getInteger(6)
                )
            )
        }

        return posts
    }

    override suspend fun closeTickets(
        selectedTickets: JsonArray,
        sqlConnection: SqlConnection
    ) {
        val parameters = Tuple.tuple()

        parameters.addInteger(3)

        var selectedTicketsSQLText = ""

        selectedTickets.forEach {
            if (selectedTicketsSQLText.isEmpty())
                selectedTicketsSQLText = "?"
            else
                selectedTicketsSQLText += ", ?"

            parameters.addValue(it)
        }

        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET status = ? WHERE id IN ($selectedTicketsSQLText)"

        sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()
    }

    override suspend fun countByCategory(
        id: Int,
        sqlConnection: SqlConnection
    ): Int {
        val query = "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` WHERE category_id = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun delete(
        ticketList: JsonArray,
        sqlConnection: SqlConnection
    ) {

        val parameters = Tuple.tuple()

        var selectedTicketsSQLText = ""

        ticketList.forEach {
            if (selectedTicketsSQLText.isEmpty())
                selectedTicketsSQLText = "?"
            else
                selectedTicketsSQLText += ", ?"

            parameters.addValue(it)
        }

        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE id IN ($selectedTicketsSQLText)"

        sqlConnection
            .preparedQuery(query)
            .execute(parameters)
            .await()
    }

    override suspend fun countByUserID(
        id: Int,
        sqlConnection: SqlConnection
    ): Int {
        val query =
            "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` where user_id = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()

        return rows.toList()[0].getInteger(0)
    }

    override suspend fun getByID(
        id: Int,
        sqlConnection: SqlConnection
    ): Ticket? {
        val query =
            "SELECT `id`, `title`, `category_id`, `user_id`, `date`, `last_update`, `status` FROM `${getTablePrefix() + tableName}` WHERE  `id` = ?"

        val rows: RowSet<Row> = sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        val ticket = Ticket(
            id = row.getInteger(0),
            title = row.getString(1),
            categoryID = row.getInteger(2),
            userID = row.getInteger(3),
            date = row.getLong(4),
            lastUpdate = row.getLong(5),
            status = row.getInteger(6),
        )

        return ticket
    }

    override suspend fun isExistsByID(
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

    override suspend fun makeStatus(
        id: Int,
        status: Int,
        sqlConnection: SqlConnection
    ) {
        val query = "UPDATE `${getTablePrefix() + tableName}` SET status = ? WHERE id = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    status,
                    id
                )
            )
            .await()
    }

    override suspend fun updateLastUpdateDate(
        id: Int,
        date: Long,
        sqlConnection: SqlConnection
    ) {
        val query = "UPDATE `${getTablePrefix() + tableName}` SET last_update = ? WHERE id = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    date,
                    id
                )
            )
            .await()
    }
}