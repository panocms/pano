package com.panomc.platform.route.api.auth

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.platform.token.TokenProvider
import com.panomc.platform.token.TokenType
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas

@Endpoint
class ValidateEmailAPI(
    private val databaseManager: DatabaseManager,
    private val tokenProvider: TokenProvider,
    private val authProvider: AuthProvider
) : Api() {
    override val paths = listOf(Path("/api/auth/validateEmail", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                Bodies.json(
                    Schemas.objectSchema()
                        .property("token", Schemas.stringSchema())
//                TODO: Add recaptcha
                )
            )
            .build()

    override suspend fun handler(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val token = data.getString("token")

        validateInput(token)

        val sqlConnection = createConnection(context)

        val isValid = tokenProvider.isTokenValid(token, TokenType.ACTIVATION, sqlConnection)

        if (!isValid) {
            throw Error(ErrorCode.INVALID_LINK)
        }

        val userId = authProvider.getUserIdFromToken(token)

        databaseManager.userDao.makeEmailVerifiedById(userId, sqlConnection)

        tokenProvider.invalidateTokensBySubjectAndType(userId.toString(), TokenType.ACTIVATION, sqlConnection)

        return Successful()
    }

    private fun validateInput(token: String) {
        if (token.isBlank()) {
            throw Error(ErrorCode.INVALID_LINK)
        }
    }
}