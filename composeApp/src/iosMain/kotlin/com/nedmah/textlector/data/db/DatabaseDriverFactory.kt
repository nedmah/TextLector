package com.nedmah.textlector.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.nedmah.textlector.db.LectorDatabase

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver = NativeSqliteDriver(LectorDatabase.Schema,"lector.db")

}