package com.panomc.platform.model

import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.InstallationRequired
import com.panomc.platform.error.NotLoggedIn
import com.panomc.platform.setup.SetupManager
import io.vertx.ext.web.RoutingContext
import org.springframework.beans.factory.annotation.Autowired

abstract class LoggedInApi : Api() {
    @Autowired
    private lateinit var databaseManager: DatabaseManager

    @Autowired
    private lateinit var setupManager: SetupManager

    @Autowired
    private lateinit var authProvider: AuthProvider

    private fun checkSetup() {
        if (!setupManager.isSetupDone()) {
            throw InstallationRequired()
        }
    }

    private suspend fun checkLoggedIn(context: RoutingContext) {
        val isLoggedIn = authProvider.isLoggedIn(context)

        if (!isLoggedIn) {
            throw NotLoggedIn()
        }
    }

    private suspend fun updateLastActivityTime(context: RoutingContext) {
        val userId = authProvider.getUserIdFromRoutingContext(context)
        val sqlClient = databaseManager.getSqlClient()

        databaseManager.userDao.updateLastActivityTime(userId, sqlClient)
    }

    override suspend fun onBeforeHandle(context: RoutingContext) {
        checkSetup()

        checkLoggedIn(context)

        updateLastActivityTime(context)
    }
}