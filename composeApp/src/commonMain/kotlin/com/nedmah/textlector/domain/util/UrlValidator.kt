package com.nedmah.textlector.domain.util

object UrlValidator {

    private val URL_REGEX = Regex(
        "^(https?://)?" +           // scheme
                "([\\w\\-]+\\.)+[\\w]{2,}" + // domen
                "(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?" + // path
                "$"
    )

    sealed class UrlError {
        data object Empty : UrlError()
        data object NoScheme : UrlError()
        data object InvalidFormat : UrlError()
    }

    fun validate(url: String): UrlError? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return UrlError.Empty
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return UrlError.NoScheme
        }
        if (!URL_REGEX.matches(trimmed)) return UrlError.InvalidFormat
        return null
    }

    fun errorMessage(error: UrlError): String = when (error) {
        UrlError.Empty -> "Enter a URL"
        UrlError.NoScheme -> "URL must start with http:// or https://"
        UrlError.InvalidFormat -> "Invalid URL format"
    }
}