package com.nedmah.textlector.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.nedmah.textlector.db.LectorDatabase

actual class DatabaseDriverFactory(
    private val context: Context
) {

    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(LectorDatabase.Schema, context, "lector.db")

}