package com.panomc.platform.route.api.panel.post

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.Post
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class PanelUpdatePostAPI(
    private val databaseManager: DatabaseManager,
    setupManager: SetupManager,
    private val authProvider: AuthProvider
) : PanelApi(setupManager, authProvider) {
    override val routeType = RouteType.PUT

    override val routes = arrayListOf("/api/panel/posts/:id")

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandler.builder(schemaParser)
            .pathParameter(Parameters.param("id", intSchema()))
            .body(
                json(
                    objectSchema()
                        .property("title", stringSchema())
                        .property("category", intSchema())
                        .property("text", stringSchema())
                        .optionalProperty("imageCode", stringSchema())
                )
            )
            .build()

    override suspend fun handler(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val id = parameters.pathParameter("id").integer
        val title = data.getString("title")
        val categoryId = data.getInteger("category")
        val text = data.getString("text")
        val imageCode = data.getString("imageCode") ?: ""

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val sqlConnection = createConnection(databaseManager, context)

        val exists = databaseManager.postDao.isExistsById(id, sqlConnection)

        if (!exists) {
            throw Error(ErrorCode.NOT_EXISTS)
        }

        val post = Post(
            id,
            title,
            categoryId,
            userId,
            text,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            1,
            imageCode,
            0
        )

        databaseManager.postDao.updateAndPublish(userId, post, sqlConnection)

        return Successful()
    }
}