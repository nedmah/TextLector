package com.nedmah.textlector.di

import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.DocumentRepositoryImpl
import com.nedmah.textlector.data.repository.ParagraphRepositoryImpl
import com.nedmah.textlector.db.LectorDatabase
import com.nedmah.textlector.domain.repository.DocumentRepository
import com.nedmah.textlector.domain.repository.ParagraphRepository
import org.koin.dsl.module

val dataModule = module {

    single { get<DatabaseDriverFactory>().createDriver() }

    single { LectorDatabase(get()) }

    single { get<LectorDatabase>().documentQueries }
    single { get<LectorDatabase>().paragraphQueries }

    single<DocumentRepository> { DocumentRepositoryImpl(get()) }
    single<ParagraphRepository> { ParagraphRepositoryImpl(get()) }
}