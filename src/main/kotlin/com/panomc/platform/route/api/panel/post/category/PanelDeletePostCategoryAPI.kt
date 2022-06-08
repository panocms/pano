package com.panomc.platform.route.api.panel.post.category

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.intSchema

@Endpoint
class PanelDeletePostCategoryAPI(
    private val databaseManager: DatabaseManager,
    setupManager: SetupManager,
    authProvider: AuthProvider
) : PanelApi(setupManager, authProvider) {
    override val routeType = RouteType.DELETE

    override val routes = arrayListOf("/api/panel/post/categories/:id")

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandler.builder(schemaParser)
            .pathParameter(param("id", intSchema()))
            .build()

    override suspend fun handler(context: RoutingContext): Result {
        val parameters = getParameters(context)

        val id = parameters.pathParameter("id").integer

        val sqlConnection = createConnection(databaseManager, context)

        val exists = databaseManager.postCategoryDao.isExistsById(id, sqlConnection)

        if (!exists) {
            throw Error(ErrorCode.NOT_EXISTS)
        }

        databaseManager.postDao.removePostCategoriesByCategoryId(id, sqlConnection)

        databaseManager.postCategoryDao.deleteById(id, sqlConnection)

        return Successful()
    }
}