package no.fintlabs.adapter.core

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * A tiny FINT gateway plus IDP on a random port, recording what adapters send it.
 */
class StubGateway : AutoCloseable {
    data class Call(val method: String, val path: String, val bearer: String?, val body: String)

    val calls = ConcurrentLinkedQueue<Call>()
    val tokenRequests = AtomicInteger()
    /** Pending events keyed by the bearer token of the credential that holds the contract for them. */
    private val pendingEvents = ConcurrentLinkedQueue<Pair<String, String>>()

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/token") { exchange ->
            val body = exchange.requestBody.readAllBytes().decodeToString()
            tokenRequests.incrementAndGet()
            val username = body.split("&").first { it.startsWith("username=") }.substringAfter("=")
            respond(exchange, 200, """{"access_token":"token-for-$username","expires_in":3600}""")
        }
        createContext("/provider") { exchange ->
            val body = exchange.requestBody.readAllBytes().decodeToString()
            val bearer = exchange.requestHeaders.getFirst("Authorization")?.removePrefix("Bearer ")
            calls += Call(exchange.requestMethod, exchange.requestURI.path, bearer, body)
            when {
                exchange.requestMethod == "GET" && exchange.requestURI.path.startsWith("/provider/event/") -> {
                    val mine = pendingEvents.filter { it.first == bearer }
                    pendingEvents.removeAll(mine.toSet())
                    respond(exchange, 200, "[" + mine.joinToString(",") { it.second } + "]")
                }
                else -> respond(exchange, 200, "")
            }
        }
        start()
    }

    val baseUrl = "http://127.0.0.1:${server.address.port}"
    val tokenUri = "$baseUrl/token"

    /** Like the gateway, an event is only served to the credential that has the contract for its org. */
    fun queueEvent(bearer: String, json: String) = pendingEvents.add(bearer to json)

    fun calls(method: String, path: String) = calls.filter { it.method == method && it.path == path }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    override fun close() = server.stop(0)
}
