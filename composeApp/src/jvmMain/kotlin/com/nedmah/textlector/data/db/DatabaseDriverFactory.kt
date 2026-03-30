package com.nedmah.textlector.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nedmah.textlector.db.LectorDatabase

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver : SqlDriver = JdbcSqliteDriver("jdbc:sqlite:lector.db")
        LectorDatabase.Schema.create(driver)
        return driver
    }

}