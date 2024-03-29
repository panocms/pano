package com.panomc.platform.route.api.ticket

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.model.LoggedInApi
import com.panomc.platform.model.Path
import com.panomc.platform.model.Result
import com.panomc.platform.model.RouteType
import com.panomc.platform.util.TicketPageType
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.optionalParam
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.json.schema.common.dsl.Schemas.arraySchema

@Endpoint
class GetTicketsAPI(
    private val getTicketsService: GetTicketsService
) : LoggedInApi() {
    override val paths = listOf(Path("/api/tickets", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .queryParameter(
                optionalParam(
                    "pageType",
                    arraySchema()
                        .items(
                            Schemas.enumSchema(
                                *TicketPageType.entries
                                    .map { it.name }
                                    .toTypedArray()
                            )
                        )
                )
            )
            .queryParameter(optionalParam("page", Schemas.numberSchema()))
            .queryParameter(optionalParam("categoryUrl", Schemas.stringSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val sqlClient = getSqlClient()
        val parameters = getParameters(context)

        return getTicketsService.handle(context, sqlClient, parameters)
    }
}