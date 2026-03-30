package com.nedmah.textlector.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver() : SqlDriver
}