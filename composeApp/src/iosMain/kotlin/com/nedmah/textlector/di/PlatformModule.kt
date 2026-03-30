package com.nedmah.textlector.di

import com.nedmah.textlector.data.db.DatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
}