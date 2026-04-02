package com.nedmah.textlector.common.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ImportRoute

@Serializable
data class ReaderRoute(val documentId: String)

@Serializable
data object LibraryRoute

@Serializable
data object SettingsRoute