package app.lang.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HTTP client for online features (the AI tutor first). The engine comes from the platform
 * dependency: OkHttp on Android, the JDK HTTP client on Windows.
 */
fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 60_000
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )
    }
}
