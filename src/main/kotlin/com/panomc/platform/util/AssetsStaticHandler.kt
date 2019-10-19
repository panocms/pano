package com.panomc.platform.util

import com.panomc.platform.Main.Companion.getComponent
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.impl.StaticHandlerImpl
import javax.inject.Inject

class AssetsStaticHandler(private val mRoot: String) : StaticHandlerImpl(mRoot, null) {

    @Inject
    lateinit var setupManager: SetupManager

    @Inject
    lateinit var configManager: ConfigManager

    init {
        getComponent().inject(this)
    }

    override fun handle(context: RoutingContext) {
        if (context.normalisedPath().startsWith("/panel/")) {
            val auth = Auth()

            auth.isAdmin(context) { isAdmin ->
                handle(context, isAdmin)
            }
        } else
            handle(context, false)
    }

    private fun handle(context: RoutingContext, isAdmin: Boolean) {
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var assetsFolderRoot = if (setupManager.isSetupDone())
            "view/ui/site/themes/" + configManager.config.string("current-theme") + "/assets/"
        else
            "view/ui/setup/assets/"

        val normalisedPath = context.normalisedPath()

        if (normalisedPath.startsWith("/panel/") && isAdmin)
            assetsFolderRoot = "view/ui/site/panel/assets/"
        else if (normalisedPath.startsWith("/panel/")) {
            context.reroute("/error-404")

            return
        }

        setWebRoot(assetsFolderRoot + mRoot)

        super.handle(context)
    }
}