package com.panomc.platform.migration.database

import com.panomc.platform.util.DatabaseManager.Companion.DatabaseMigration
import io.vertx.core.AsyncResult
import io.vertx.ext.sql.SQLConnection

@Suppress("ClassName")
class DatabaseMigration_10_11 : DatabaseMigration {
    override val FROM_SCHEME_VERSION = 10
    override val SCHEME_VERSION = 11
    override val SCHEME_VERSION_INFO = "Add views field to post table."

    override val handlers: List<(sqlConnection: SQLConnection, tablePrefix: String, handler: (asyncResult: AsyncResult<*>) -> Unit) -> SQLConnection> =
        listOf(
            addViewsFieldToPostTable()
        )

    private fun addViewsFieldToPostTable(): (
        sqlConnection: SQLConnection,
        tablePrefix: String,
        handler: (asyncResult: AsyncResult<*>) -> Unit
    ) -> SQLConnection = { sqlConnection, tablePrefix, handler ->
        sqlConnection.query(
            """
                    ALTER TABLE `${tablePrefix}post` 
                    ADD `views` MEDIUMTEXT;
                """
        ) {
            handler.invoke(it)
        }
    }
}