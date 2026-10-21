package no.fintlabs.adapter.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)

/** Message plus response body when the gateway answered with one, so a 4xx is explained in the log. */
fun Throwable.describe(): String =
    if (this is WebClientResponseException && responseBodyAsString.isNotEmpty()) {
        "$message - response body: $responseBodyAsString"
    } else {
        message ?: toString()
    }
