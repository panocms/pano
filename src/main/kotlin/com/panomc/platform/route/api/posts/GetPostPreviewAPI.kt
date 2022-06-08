package com.panomc.platform.route.api.posts

import com.panomc.platform.ErrorCode
import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.PostCategory
import com.panomc.platform.model.*
import com.panomc.platform.util.AuthProvider
import com.panomc.platform.util.SetupManager
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.intSchema

@Endpoint
class GetPostPreviewAPI(
    private val databaseManager: DatabaseManager,
    setupManager: SetupManager,
    authProvider: AuthProvider
) : PanelApi(setupManager, authProvider) {
    override val routeType = RouteType.GET

    override val routes = arrayListOf("/api/panel/posts/:id/preview")

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandler.builder(schemaParser)
            .pathParameter(param("id", intSchema()))
            .build()

    override suspend fun handler(context: RoutingContext): Result {
        val parameters = getParameters(context)

        val id = parameters.pathParameter("id").integer

        val sqlConnection = createConnection(databaseManager, context)

        val isPostExistsById = databaseManager.postDao.isExistsById(id, sqlConnection)

        if (!isPostExistsById) {
            throw Error(ErrorCode.POST_NOT_FOUND)
        }

        val post = databaseManager.postDao.getById(id, sqlConnection) ?: throw Error(ErrorCode.UNKNOWN)
        var postCategory: PostCategory? = null

        if (post.categoryId != -1) {
            postCategory = databaseManager.postCategoryDao.getById(post.categoryId, sqlConnection)
                ?: throw Error(ErrorCode.UNKNOWN)
        }

        val username = databaseManager.userDao.getUsernameFromUserId(post.writerUserId, sqlConnection)
            ?: throw Error(ErrorCode.UNKNOWN)

        return Successful(
            mapOf(
                "id" to post.id,
                "title" to post.title,
                "category" to
                        if (postCategory == null)
                            mapOf("id" to -1, "title" to "-")
                        else
                            mapOf<String, Any?>(
                                "title" to postCategory.title,
                                "url" to postCategory.url
                            ),
                "writer" to mapOf(
                    "username" to username
                ),
                "text" to post.text,
                "date" to post.date,
                "status" to post.status,
                "image" to post.image,
                "views" to post.views
            )
        )
    }
}