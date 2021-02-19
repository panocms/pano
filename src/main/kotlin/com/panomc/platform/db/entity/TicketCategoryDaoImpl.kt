package com.panomc.platform.db.entity

import com.panomc.platform.db.DaoImpl
import com.panomc.platform.db.dao.TicketCategoryDao
import com.panomc.platform.db.model.TicketCategory
import com.panomc.platform.model.Result
import com.panomc.platform.model.Successful
import io.vertx.core.AsyncResult
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

class TicketCategoryDaoImpl(override val tableName: String = "ticket_category") : DaoImpl(), TicketCategoryDao {

    override fun init(): (sqlConnection: SqlConnection, handler: (asyncResult: AsyncResult<*>) -> Unit) -> Unit =
        { sqlConnection, handler ->
            sqlConnection
                .query(
                    """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `title` MEDIUMTEXT NOT NULL,
                              `description` text,
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ticket category table.';
                        """
                )
                .execute {
                    handler.invoke(it)
                }
        }

    override fun getAll(
        sqlConnection: SqlConnection,
        handler: (categories: List<TicketCategory>?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query = "SELECT id, title FROM `${getTablePrefix() + tableName}`"
        val categories = mutableListOf<TicketCategory>()

        sqlConnection
            .preparedQuery(query)
            .execute { queryResult ->
                if (queryResult.failed()) {
                    handler.invoke(null, queryResult)

                    return@execute
                }

                val rows: RowSet<Row> = queryResult.result()

                rows.forEach { row ->
                    categories.add(
                        TicketCategory(
                            row.getInteger(0),
                            row.getString(1)
                        )
                    )
                }

                handler.invoke(categories, queryResult)
            }
    }

    override fun isExistsByID(
        id: Int,
        sqlConnection: SqlConnection,
        handler: (exists: Boolean?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query = "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}` where `id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id)) { queryResult ->
                if (queryResult.succeeded()) {
                    val rows: RowSet<Row> = queryResult.result()

                    handler.invoke(rows.toList()[0].getInteger(0) == 1, queryResult)
                } else
                    handler.invoke(null, queryResult)
            }
    }

    override fun deleteByID(
        id: Int,
        sqlConnection: SqlConnection,
        handler: (result: Result?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query = "DELETE FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id)) { queryResult ->
                if (queryResult.succeeded())
                    handler.invoke(Successful(), queryResult)
                else
                    handler.invoke(null, queryResult)
            }
    }

    override fun add(
        ticketCategory: TicketCategory,
        sqlConnection: SqlConnection,
        handler: (result: Result?, asyncResult: AsyncResult<*>) -> Unit
    ) {

        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (`title`, `description`) VALUES (?, ?)"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    ticketCategory.title,
                    ticketCategory.description
                )
            ) { queryResult ->
                if (queryResult.succeeded())
                    handler.invoke(Successful(), queryResult)
                else
                    handler.invoke(null, queryResult)
            }
    }

    override fun update(
        ticketCategory: TicketCategory,
        sqlConnection: SqlConnection,
        handler: (result: Result?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET title = ?, description = ? WHERE `id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    ticketCategory.title,
                    ticketCategory.description,
                    ticketCategory.id
                )
            ) { queryResult ->
                if (queryResult.succeeded())
                    handler.invoke(Successful(), queryResult)
                else
                    handler.invoke(null, queryResult)
            }
    }

    override fun count(sqlConnection: SqlConnection, handler: (count: Int?, asyncResult: AsyncResult<*>) -> Unit) {
        val query =
            "SELECT COUNT(id) FROM `${getTablePrefix() + tableName}`"

        sqlConnection
            .preparedQuery(query)
            .execute { queryResult ->
                if (queryResult.succeeded()) {
                    val rows: RowSet<Row> = queryResult.result()

                    handler.invoke(rows.toList()[0].getInteger(0), queryResult)
                } else
                    handler.invoke(null, queryResult)
            }
    }

    override fun getByPage(
        page: Int,
        sqlConnection: SqlConnection,
        handler: (categories: List<Map<String, Any>>?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query =
            "SELECT id, title, description FROM `${getTablePrefix() + tableName}` ORDER BY id DESC LIMIT 10 OFFSET ${(page - 1) * 10}"

        sqlConnection
            .preparedQuery(query)
            .execute { queryResult ->
                if (queryResult.succeeded()) {
                    val rows: RowSet<Row> = queryResult.result()
                    val categories = mutableListOf<Map<String, Any>>()

                    if (rows.size() > 0) {
                        val handlers: List<(handler: () -> Unit) -> Any> =
                            rows.map { row ->
                                val localHandler: (handler: () -> Unit) -> Any = { localHandler ->
                                    databaseManager.getDatabase().ticketDao.countByCategory(
                                        row.getInteger(0),
                                        sqlConnection
                                    ) { count, asyncResult ->
                                        if (count == null) {
                                            handler.invoke(null, asyncResult)

                                            return@countByCategory
                                        }

                                        databaseManager.getDatabase().ticketDao.getByCategory(
                                            row.getInteger(0),
                                            sqlConnection
                                        ) { tickets, asyncResultOfGetByCategory ->
                                            if (tickets == null) {
                                                handler.invoke(null, asyncResultOfGetByCategory)

                                                return@getByCategory
                                            }

                                            categories.add(
                                                mapOf(
                                                    "id" to row.getInteger(0),
                                                    "title" to row.getString(1),
                                                    "description" to row.getString(2),
                                                    "ticket_count" to count,
                                                    "tickets" to tickets
                                                )
                                            )

                                            localHandler.invoke()
                                        }
                                    }
                                }

                                localHandler
                            }

                        var currentIndex = -1

                        fun invoke() {
                            val localHandler: () -> Unit = {
                                if (currentIndex == handlers.lastIndex)
                                    handler.invoke(categories, queryResult)
                                else
                                    invoke()
                            }

                            currentIndex++

                            if (currentIndex <= handlers.lastIndex)
                                handlers[currentIndex].invoke(localHandler)
                        }

                        invoke()
                    } else
                        handler.invoke(categories, queryResult)
                } else
                    handler.invoke(null, queryResult)
            }
    }

    override fun getByID(
        id: Int,
        sqlConnection: SqlConnection,
        handler: (ticketCategory: TicketCategory?, asyncResult: AsyncResult<*>) -> Unit
    ) {
        val query =
            "SELECT `id`, `title`, `description` FROM `${getTablePrefix() + tableName}` WHERE  `id` = ?"

        sqlConnection
            .preparedQuery(query)
            .execute(Tuple.of(id)) { queryResult ->
                if (queryResult.succeeded()) {
                    val rows: RowSet<Row> = queryResult.result()
                    val row = rows.toList()[0]

                    val ticket = TicketCategory(
                        id = row.getInteger(0),
                        title = row.getString(1),
                        description = row.getString(2),
                    )

                    handler.invoke(ticket, queryResult)
                } else
                    handler.invoke(null, queryResult)
            }
    }
}