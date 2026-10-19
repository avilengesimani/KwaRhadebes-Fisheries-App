package za.co.kwarhadebe.fisheries

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AuthSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val accessToken: String
)

object SupabaseAuthRepository {

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        accessToken: String? = null,
        prefer: String? = null
    ): String {
        val connection = (
                URL("${SupabaseConfig.PROJECT_URL}$path")
                    .openConnection() as HttpURLConnection
                )

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty(
                "apikey",
                SupabaseConfig.PUBLISHABLE_KEY
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }

            if (!accessToken.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )
            }

            if (prefer != null) {
                connection.setRequestProperty("Prefer", prefer)
            }

            if (body != null) {
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()
                ?.use { it.readText() } ?: ""

            if (status !in 200..299) {
                val message = try {
                    val json = JSONObject(response)
                    json.optString(
                        "msg",
                        json.optString(
                            "message",
                            json.optString(
                                "error_description",
                                json.optString("error", response)
                            )
                        )
                    )
                } catch (_: Exception) {
                    response
                }

                throw IOException("HTTP $status: $message")
            }

            return response
        } finally {
            connection.disconnect()
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String
    ): AuthSession? = withContext(Dispatchers.IO) {

        val body = JSONObject()
        body.put("email", email.trim())
        body.put("password", password)

        val metadata = JSONObject()
        metadata.put("full_name", fullName.trim())
        body.put("data", metadata)

        val response = request(
            path = "/auth/v1/signup",
            method = "POST",
            body = body
        )

        val json = JSONObject(response)
        val user = json.optJSONObject("user") ?: json

        val userId = user.optString("id")
        val token = json.optString("access_token")

        if (userId.isBlank()) {
            throw IOException("Supabase did not return a user ID.")
        }

        if (token.isBlank()) {
            // Email confirmation may be required before a session exists.
            return@withContext null
        }

        val session = AuthSession(
            userId = userId,
            email = user.optString("email", email.trim()),
            fullName = fullName.trim(),
            accessToken = token
        )

        ensureCustomerProfile(session)
        session
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthSession = withContext(Dispatchers.IO) {

        val body = JSONObject()
        body.put("email", email.trim())
        body.put("password", password)

        val response = request(
            path = "/auth/v1/token?grant_type=password",
            method = "POST",
            body = body
        )

        val json = JSONObject(response)
        val user = json.optJSONObject("user")
            ?: throw IOException("Supabase did not return a user.")

        val userId = user.optString("id")
        val token = json.optString("access_token")

        if (userId.isBlank() || token.isBlank()) {
            throw IOException("The login response is missing session details.")
        }

        val metadata = user.optJSONObject("user_metadata")
        val fullName = metadata?.optString("full_name")
            ?.takeIf { !it.isNullOrBlank() }
            ?: email.substringBefore("@")

        val session = AuthSession(
            userId = userId,
            email = user.optString("email", email.trim()),
            fullName = fullName,
            accessToken = token
        )

        ensureCustomerProfile(session)
        session
    }

    private suspend fun ensureCustomerProfile(
        session: AuthSession
    ) = withContext(Dispatchers.IO) {

        val encodedId = URLEncoder.encode(
            session.userId,
            "UTF-8"
        )

        val path =
            "/rest/v1/customers" +
                    "?auth_user_id=eq.$encodedId" +
                    "&select=customer_id"

        val response = request(
            path = path,
            method = "GET",
            accessToken = session.accessToken
        )

        if (org.json.JSONArray(response).length() > 0) {
            return@withContext
        }

        val profile = JSONObject()
        profile.put("auth_user_id", session.userId)
        profile.put("full_name", session.fullName)
        profile.put("email", session.email)

        request(
            path = "/rest/v1/customers",
            method = "POST",
            body = profile,
            accessToken = session.accessToken,
            prefer = "return=minimal"
        )
    }
}