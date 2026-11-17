package com.nedmah.textlector.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nedmah.textlector.common.platform.file.AppDirs
import com.nedmah.textlector.db.LectorDatabase
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val dbFile = File(AppDirs.dataDir, "lector.db")
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
            schema = LectorDatabase.Schema,
        )
    }

}