package com.nedmah.textlector.di

import com.nedmah.textlector.data.db.DatabaseDriverFactory
import com.nedmah.textlector.data.repository.DocumentRepositoryImpl
import com.nedmah.textlector.data.repository.ParagraphRepositoryImpl
import com.nedmah.textlector.data.repository.PreferencesRepositoryImpl
import com.nedmah.textlector.db.LectorDatabase
import com.nedmah.textlector.domain.repository.DocumentRepository
import com.nedmah.textlector.domain.repository.ParagraphRepository
import com.nedmah.textlector.domain.repository.PreferencesRepository
import com.nedmah.textlector.domain.usecase.DeleteDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetDocumentUseCase
import com.nedmah.textlector.domain.usecase.GetDocumentsUseCase
import com.nedmah.textlector.domain.usecase.GetFavoritesUseCase
import com.nedmah.textlector.domain.usecase.GetParagraphsUseCase
import com.nedmah.textlector.domain.usecase.GetPreferencesUseCase
import com.nedmah.textlector.domain.usecase.GetRecentDocumentsUseCase
import com.nedmah.textlector.domain.usecase.ImportDocumentUseCase
import com.nedmah.textlector.domain.usecase.InputTextManuallyUseCase
import com.nedmah.textlector.domain.usecase.SaveProgressUseCase
import com.nedmah.textlector.domain.usecase.ToggleFavoriteUseCase
import com.nedmah.textlector.domain.usecase.UpdateLastOpenedUseCase
import com.nedmah.textlector.domain.usecase.UpdatePreferencesUseCase
import org.koin.dsl.module

val dataModule = module {

    single { get<DatabaseDriverFactory>().createDriver() }

    single { LectorDatabase(get()) }

    single { get<LectorDatabase>().documentQueries }
    single { get<LectorDatabase>().paragraphQueries }

    single<DocumentRepository> { DocumentRepositoryImpl(get()) }
    single<ParagraphRepository> { ParagraphRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    // UseCases
    factory { GetDocumentsUseCase(get()) }
    factory { GetDocumentUseCase(get()) }
    factory { GetFavoritesUseCase(get()) }
    factory { GetRecentDocumentsUseCase(get()) }
    factory { GetParagraphsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { DeleteDocumentUseCase(get()) }
    factory { SaveProgressUseCase(get()) }
    factory { UpdateLastOpenedUseCase(get()) }
    factory { GetPreferencesUseCase(get()) }
    factory { UpdatePreferencesUseCase(get()) }
    factory { InputTextManuallyUseCase(get(), get()) }
    factory { ImportDocumentUseCase(get(), get(), get()) }
}