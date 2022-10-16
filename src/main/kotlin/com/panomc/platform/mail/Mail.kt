package com.panomc.platform.mail

import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.util.TokenProvider
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

interface Mail {
    val templatePath: String

    val subject: String

    suspend fun parameterGenerator(
        email: String,
        userId: Long,
        uiAddress: String,
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        tokenProvider: TokenProvider
    ): JsonObject
}