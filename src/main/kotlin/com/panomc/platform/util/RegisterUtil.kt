package com.panomc.platform.util

import com.panomc.platform.ErrorCode
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.PermissionGroup
import com.panomc.platform.db.model.SystemProperty
import com.panomc.platform.db.model.User
import com.panomc.platform.model.Error
import com.panomc.platform.model.Result
import com.panomc.platform.model.Successful
import de.triology.recaptchav2java.ReCaptcha
import io.vertx.core.AsyncResult
import io.vertx.sqlclient.SqlConnection

object RegisterUtil {

    fun validateForm(
        username: String,
        email: String,
        password: String,
        passwordRepeat: String = password,
        agreement: Boolean,
        recaptchaToken: String = "",
        reCaptcha: ReCaptcha? = null,
        handler: (result: Result) -> Unit
    ) {
        if (username.isEmpty()) {
            handler.invoke(Error(ErrorCode.REGISTER_USERNAME_EMPTY))

            return
        }

        if (email.isEmpty()) {
            handler.invoke(Error(ErrorCode.REGISTER_EMAIL_EMPTY))

            return
        }

        if (password.isEmpty()) {
            handler.invoke(Error(ErrorCode.REGISTER_PASSWORD_EMPTY))

            return
        }

        if (username.length < 3) {
            handler.invoke(Error(ErrorCode.REGISTER_USERNAME_TOO_SHORT))

            return
        }

        if (username.length > 16) {
            handler.invoke(Error(ErrorCode.REGISTER_USERNAME_TOO_LONG))

            return
        }

        if (password.length < 6) {
            handler.invoke(Error(ErrorCode.REGISTER_PASSWORD_TOO_SHORT))

            return
        }

        if (password.length > 128) {
            handler.invoke(Error(ErrorCode.REGISTER_PASSWORD_TOO_LONG))

            return
        }

        if (!username.matches(Regex("^[a-zA-Z0-9_]+\$"))) {
            handler.invoke(Error(ErrorCode.REGISTER_INVALID_USERNAME))

            return
        }

        if (!email.matches(Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}\$"))) {
            handler.invoke(Error(ErrorCode.REGISTER_INVALID_USERNAME))

            return
        }
        if (password != passwordRepeat) {
            handler.invoke(Error(ErrorCode.REGISTER_PASSWORD_AND_PASSWORD_REPEAT_NOT_SAME))

            return
        }

        if (!agreement) {
            handler.invoke(Error(ErrorCode.REGISTER_NOT_ACCEPTED_AGREEMENT))
        }

        if (reCaptcha != null && !reCaptcha.isValid(recaptchaToken)) {
            handler.invoke(Error(ErrorCode.REGISTER_CANT_VERIFY_ROBOT))

            return
        }

        handler.invoke(Successful())
    }

    fun register(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        username: String,
        email: String,
        password: String,
        remoteIP: String,
        isAdmin: Boolean = false,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit
    ) {
        databaseManager.getDatabase().userDao.isExistsByUsername(
            username,
            sqlConnection,
            (this::isExistsByUsernameHandler)(
                databaseManager,
                sqlConnection,
                handler,
                username,
                email,
                password,
                remoteIP,
                isAdmin
            )
        )
    }

    private fun isExistsByUsernameHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit,
        username: String,
        email: String,
        password: String,
        remoteIP: String,
        isAdmin: Boolean
    ) = handler@{ exists: Boolean?, asyncResult: AsyncResult<*> ->
        if (exists == null) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_244), asyncResult)
            }

            return@handler
        }

        if (exists) {
            databaseManager.closeConnection(sqlConnection) {
                handler.invoke(Error(ErrorCode.REGISTER_USERNAME_NOT_AVAILABLE), null)
            }

            return@handler
        }

        databaseManager.getDatabase().userDao.isEmailExists(
            email,
            sqlConnection,
            (this::isEmailExistsHandler)(
                databaseManager,
                sqlConnection,
                handler,
                username,
                email,
                password,
                remoteIP,
                isAdmin
            )
        )
    }

    private fun isEmailExistsHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit,
        username: String,
        email: String,
        password: String,
        remoteIP: String,
        isAdmin: Boolean
    ) = handler@{ exists: Boolean?, asyncResult: AsyncResult<*> ->
        if (exists == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_1),
                asyncResult
            )

            return@handler
        }

        if (exists) {
            handler.invoke(Error(ErrorCode.REGISTER_EMAIL_NOT_AVAILABLE), null)

            return@handler
        }

        val user = User(-1, username, email, password, remoteIP, -1, System.currentTimeMillis())

        if (!isAdmin) {
            addUser(user, databaseManager, sqlConnection, handler)

            return@handler
        }

        databaseManager.getDatabase().permissionGroupDao.getPermissionGroupID(
            PermissionGroup(-1, "admin"),
            sqlConnection,
            (this::getPermissionGroupIDHandler)(
                databaseManager,
                sqlConnection,
                handler,
                username,
                email,
                password,
                remoteIP
            )
        )
    }

    private fun getPermissionGroupIDHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit,
        username: String,
        email: String,
        password: String,
        remoteIP: String
    ) = handler@{ permissionGroupID: Int?, asyncResult: AsyncResult<*> ->
        if (permissionGroupID == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_3),
                asyncResult
            )

            return@handler
        }

        val adminUser = User(
            -1,
            username,
            email,
            password,
            remoteIP,
            permissionGroupID,
            System.currentTimeMillis()
        )

        addUser(
            adminUser,
            databaseManager,
            sqlConnection,
            (this::addUserHandler)(databaseManager, sqlConnection, handler, username)
        )
    }

    private fun addUserHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit,
        username: String
    ) = handler@{ result: Result?, asyncResult: AsyncResult<*> ->
        if (result == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_157),
                asyncResult
            )

            return@handler
        }

        databaseManager.getDatabase().userDao.getUserIDFromUsername(
            username,
            sqlConnection,
            (this::getUserIDFromUsernameHandler)(databaseManager, sqlConnection, handler)
        )
    }

    private fun getUserIDFromUsernameHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit
    ) = handler@{ userID: Int?, asyncResult: AsyncResult<*> ->
        if (userID == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_12),
                asyncResult
            )

            return@handler
        }

        val property = SystemProperty(-1, "who_installed_user_id", userID.toString())

        databaseManager.getDatabase().systemPropertyDao.isPropertyExists(
            property,
            sqlConnection,
            (this::isPropertyExistsHandler)(databaseManager, sqlConnection, handler, property)
        )
    }

    private fun isPropertyExistsHandler(
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit,
        property: SystemProperty
    ) = handler@{ exists: Boolean?, asyncResult: AsyncResult<*> ->
        if (exists == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_13),
                asyncResult
            )

            return@handler
        }

        if (exists) {
            databaseManager.getDatabase().systemPropertyDao.update(
                property,
                sqlConnection,
                (this::updateHandler)(handler)
            )

            return@handler
        }

        databaseManager.getDatabase().systemPropertyDao.add(
            property,
            sqlConnection,
            (this::addHandler)(handler)
        )
    }

    private fun updateHandler(
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit
    ) = handler@{ result: Result?, asyncResult: AsyncResult<*> ->
        if (result == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_15),
                asyncResult
            )

            return@handler
        }

        handler.invoke(Successful(), asyncResult)
    }

    private fun addHandler(
        handler: (result: Result, asyncResult: AsyncResult<*>?) -> Unit
    ) = handler@{ result: Result?, asyncResult: AsyncResult<*> ->
        if (result == null) {
            handler.invoke(
                Error(ErrorCode.UNKNOWN_ERROR_14),
                asyncResult
            )

            return@handler
        }

        handler.invoke(Successful(), asyncResult)
    }

    private fun addUser(
        user: User,
        databaseManager: DatabaseManager,
        sqlConnection: SqlConnection,
        handler: (result: Result, asyncResult: AsyncResult<*>) -> Unit
    ) {
        databaseManager.getDatabase().userDao.add(user, sqlConnection) { isSuccessful, asyncResultOfAdd ->
            if (isSuccessful == null) {
                handler.invoke(Error(ErrorCode.UNKNOWN_ERROR_144), asyncResultOfAdd)

                return@add
            }

            handler.invoke(Successful(), asyncResultOfAdd)
        }
    }
}