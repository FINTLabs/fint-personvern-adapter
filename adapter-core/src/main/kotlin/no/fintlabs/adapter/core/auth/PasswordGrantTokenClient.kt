package no.fintlabs.adapter.core.auth

import com.fasterxml.jackson.annotation.JsonProperty
import no.fintlabs.adapter.core.config.TenantProperties
import no.fintlabs.adapter.core.describe
import no.fintlabs.adapter.core.logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches and caches one access token per tenant with the OAuth2 password grant the FINT IDP uses.
 *
 * Spring Security dropped the password grant, so this is a small client of its own rather than an
 * `OAuth2AuthorizedClientManager`. A token is reused until shortly before it expires, and dropped on a 401.
 */
class PasswordGrantTokenClient(
    private val tokenUri: String,
    private val webClient: WebClient,
    private val clockSkew: Duration = Duration.ofSeconds(60),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = logger<PasswordGrantTokenClient>()
    private val tokens = ConcurrentHashMap<String, Mono<AccessToken>>()

    fun token(tenant: TenantProperties): Mono<String> =
        tokens.computeIfAbsent(tenant.username) { cached(tenant) }.map { it.value }

    fun evict(tenant: TenantProperties) {
        tokens.remove(tenant.username)
    }

    /** Adds `Authorization: Bearer` for [tenant] to every request, and forgets the token if the gateway rejects it. */
    fun bearerFilter(tenant: TenantProperties): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
        token(tenant).flatMap { token ->
            next.exchange(ClientRequest.from(request).headers { it.setBearerAuth(token) }.build())
        }.doOnNext { response ->
            if (response.statusCode() == HttpStatus.UNAUTHORIZED) evict(tenant)
        }
    }

    private fun cached(tenant: TenantProperties): Mono<AccessToken> =
        fetch(tenant).cache(
            { token -> Duration.between(Instant.now(clock), token.expiresAt).minus(clockSkew).coerceAtLeast(Duration.ZERO) },
            { Duration.ZERO },
            { Duration.ZERO },
        )

    private fun fetch(tenant: TenantProperties): Mono<AccessToken> =
        webClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form(tenant)))
            .retrieve()
            .bodyToMono(TokenResponse::class.java)
            .map { AccessToken(requireNotNull(it.accessToken) { "Token response had no access_token" }, Instant.now(clock).plusSeconds(it.expiresIn)) }
            .doOnError { log.error("Token request for {} failed: {}", tenant, it.describe()) }

    private fun form(tenant: TenantProperties) = LinkedMultiValueMap<String, String>().apply {
        add("grant_type", "password")
        add("client_id", tenant.clientId)
        add("client_secret", tenant.clientSecret)
        add("username", tenant.username)
        add("password", tenant.password)
        if (tenant.scope.isNotBlank()) add("scope", tenant.scope)
    }

    private fun Duration.coerceAtLeast(min: Duration): Duration = if (this < min) min else this

    private data class AccessToken(val value: String, val expiresAt: Instant)

    class TokenResponse {
        @JsonProperty("access_token")
        var accessToken: String? = null

        @JsonProperty("expires_in")
        var expiresIn: Long = 3600
    }
}
