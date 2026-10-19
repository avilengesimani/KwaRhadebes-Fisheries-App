package za.co.kwarhadebe.fisheries

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseMenuItem(
    val itemId: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val available: Boolean
)

object SupabaseMenuRepository {

    suspend fun fetchMenu(): List<SupabaseMenuItem> =
        withContext(Dispatchers.IO) {

            val endpoint =
                "${SupabaseConfig.PROJECT_URL}/rest/v1/menu_items" +
                        "?select=item_id,item_name,description,price,category,available" +
                        "&available=eq.true&order=item_id.asc"

            val connection =
                URL(endpoint).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "apikey",
                    SupabaseConfig.PUBLISHABLE_KEY
                )
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                if (status !in 200..299) {
                    throw IOException(
                        "Supabase returned HTTP $status: $response"
                    )
                }

                val json = JSONArray(response)
                val items = mutableListOf<SupabaseMenuItem>()

                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)

                    items.add(
                        SupabaseMenuItem(
                            itemId = item.getLong("item_id"),
                            name = item.getString("item_name"),
                            description = if (
                                item.isNull("description")
                            ) "" else item.getString("description"),
                            price = item.getDouble("price"),
                            category = item.getString("category"),
                            available = item.getBoolean("available")
                        )
                    )
                }

                items
            } finally {
                connection.disconnect()
            }
        }
}