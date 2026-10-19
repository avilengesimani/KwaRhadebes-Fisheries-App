package za.co.kwarhadebe.fisheries

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

data class FishItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val category: String
)

private val menuItems = listOf(
    FishItem(1, "Hake & Chips", "Fresh hake served with chips", 75.0, "Meals"),
    FishItem(2, "Fish Burger", "Fish burger with fresh toppings", 65.0, "Meals"),
    FishItem(3, "Calamari", "Crispy fried calamari", 85.0, "Seafood"),
    FishItem(4, "Prawns", "Delicious prawns", 120.0, "Seafood"),
    FishItem(5, "Snoek", "Traditional snoek", 95.0, "Seafood"),
    FishItem(6, "Chips", "Regular portion of chips", 25.0, "Sides"),
    FishItem(7, "Large Chips", "Large portion of chips", 40.0, "Sides"),
    FishItem(8, "Fish Cake", "Homemade fish cake", 20.0, "Sides"),
    FishItem(9, "Cooldrink", "Cold cooldrink", 18.0, "Drinks"),
    FishItem(10, "Water", "Bottled water", 12.0, "Drinks")
)

data class OrderLine(
    val itemId: Int,
    val name: String,
    val quantity: Int,
    val price: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FisheriesApp(applicationContext)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FisheriesApp(context: Context) {
    val preferences = remember {
        context.getSharedPreferences(
            "kwarhadebe_demo",
            Context.MODE_PRIVATE
        )
    }

    var loggedIn by remember {
        mutableStateOf(preferences.getBoolean("logged_in", false))
    }

    var currentEmail by remember {
        mutableStateOf(preferences.getString("email", "") ?: "")
    }

    var currentName by remember {
        mutableStateOf(preferences.getString("name", "") ?: "")
    }

    var liveMenuItems by remember {
        mutableStateOf(menuItems)
    }

    var menuLoading by remember {
        mutableStateOf(false)
    }

    var menuError by remember {
        mutableStateOf("")
    }

    var darkMode by remember {
        mutableStateOf(preferences.getBoolean("dark_mode", false))
    }

    var notifications by remember {
        mutableStateOf(preferences.getBoolean("notifications", true))
    }

    var selectedTab by remember {
        mutableStateOf("Home")
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    val cart = remember {
        mutableStateMapOf<Int, Int>()
    }

    var savedOrders by remember {
        mutableStateOf(
            preferences.getString("orders", "[]") ?: "[]"
        )
    }

    LaunchedEffect(Unit) {
        menuLoading = true
        menuError = ""

        try {
            val result = SupabaseMenuRepository.fetchMenu()

            if (result.isNotEmpty()) {
                liveMenuItems = result.map { item ->
                    FishItem(
                        id = item.itemId.toInt(),
                        name = item.name,
                        description = item.description,
                        price = item.price,
                        category = item.category
                    )
                }
            } else {
                menuError =
                    "No menu items were returned. Showing the demo menu."
            }
        } catch (e: Exception) {
            menuError = e.message
                ?: "Could not connect to Supabase. Showing the demo menu."
        } finally {
            menuLoading = false
        }
    }

    // Save the authenticated Supabase session.
    fun saveLogin(session: AuthSession) {
        currentName = session.fullName
        currentEmail = session.email
        loggedIn = true

        preferences.edit()
            .putBoolean("logged_in", true)
            .putString("name", session.fullName)
            .putString("email", session.email)
            .putString("user_id", session.userId)
            .putString("access_token", session.accessToken)
            .apply()
    }

    // Clear the saved authentication session.
    fun logout() {
        loggedIn = false
        currentName = ""
        currentEmail = ""

        preferences.edit()
            .putBoolean("logged_in", false)
            .remove("name")
            .remove("email")
            .remove("user_id")
            .remove("access_token")
            .apply()
    }

    fun saveOrders(value: String) {
        savedOrders = value

        preferences.edit()
            .putString("orders", value)
            .apply()
    }

    val cartTotal = liveMenuItems.sumOf { item ->
        item.price * (cart[item.id] ?: 0)
    }

    MaterialTheme(
        colorScheme = if (darkMode) {
            androidx.compose.material3.darkColorScheme()
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF176B45),
                secondary = androidx.compose.ui.graphics.Color(0xFFDAA520)
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (!loggedIn) {
                LoginScreen(
                    preferences = preferences,
                    onLogin = { session ->
                        saveLogin(session)
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        "KwaRhadebe's Fisheries",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Welcome, $currentName",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        val tabs = listOf(
                            "Home",
                            "Menu",
                            "Cart",
                            "Orders",
                            "Settings"
                        )

                        TabRow(
                            selectedTabIndex = tabs.indexOf(selectedTab)
                        ) {
                            tabs.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = {
                                        Text(tab, fontSize = 10.sp)
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            "Home" -> {
                                Text(
                                    "Fresh from the sea",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "Browse our menu and place your order."
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "Popular menu items",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                liveMenuItems.take(3).forEach { item ->
                                    MenuItemCard(
                                        item = item,
                                        quantity = cart[item.id] ?: 0,
                                        onAdd = {
                                            cart[item.id] =
                                                (cart[item.id] ?: 0) + 1
                                        },
                                        onRemove = {
                                            val quantity =
                                                (cart[item.id] ?: 0) - 1

                                            if (quantity <= 0) {
                                                cart.remove(item.id)
                                            } else {
                                                cart[item.id] = quantity
                                            }
                                        }
                                    )
                                }

                                Button(
                                    onClick = { selectedTab = "Menu" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View full menu")
                                }
                            }

                            "Menu" -> {
                                Text(
                                    "Our Menu",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    label = { Text("Search menu") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "All",
                                        "Meals",
                                        "Seafood",
                                        "Sides",
                                        "Drinks"
                                    ).forEach { category ->
                                        Button(
                                            onClick = {
                                                selectedCategory = category
                                            }
                                        ) {
                                            Text(category, fontSize = 10.sp)
                                        }
                                    }
                                }

                                if (menuLoading) {
                                    Text("Loading menu from Supabase...")
                                }

                                if (menuError.isNotBlank()) {
                                    Text(
                                        menuError,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                val filteredItems = liveMenuItems.filter {
                                    val matchesSearch =
                                        it.name.contains(
                                            searchText,
                                            ignoreCase = true
                                        ) ||
                                                it.description.contains(
                                                    searchText,
                                                    ignoreCase = true
                                                )

                                    val matchesCategory =
                                        selectedCategory == "All" ||
                                                it.category.equals(
                                                    selectedCategory,
                                                    ignoreCase = true
                                                )

                                    matchesSearch && matchesCategory
                                }

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        filteredItems,
                                        key = { it.id }
                                    ) { item ->
                                        MenuItemCard(
                                            item = item,
                                            quantity = cart[item.id] ?: 0,
                                            onAdd = {
                                                cart[item.id] =
                                                    (cart[item.id] ?: 0) + 1
                                            },
                                            onRemove = {
                                                val quantity =
                                                    (cart[item.id] ?: 0) - 1

                                                if (quantity <= 0) {
                                                    cart.remove(item.id)
                                                } else {
                                                    cart[item.id] = quantity
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            "Cart" -> {
                                Text(
                                    "Your Cart",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val cartItems = liveMenuItems.filter {
                                    (cart[it.id] ?: 0) > 0
                                }

                                if (cartItems.isEmpty()) {
                                    Text("Your cart is empty.")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            cartItems,
                                            key = { it.id }
                                        ) { item ->
                                            Card(
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(12.dp)
                                                ) {
                                                    Text(
                                                        item.name,
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    )

                                                    Text(
                                                        "R${money(item.price)} each"
                                                    )

                                                    Row(
                                                        verticalAlignment =
                                                            Alignment.CenterVertically
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                val q =
                                                                    (cart[item.id]
                                                                        ?: 0) - 1

                                                                if (q <= 0) {
                                                                    cart.remove(
                                                                        item.id
                                                                    )
                                                                } else {
                                                                    cart[item.id] =
                                                                        q
                                                                }
                                                            }
                                                        ) {
                                                            Text("−")
                                                        }

                                                        Text(
                                                            "  ${cart[item.id] ?: 0}  "
                                                        )

                                                        Button(
                                                            onClick = {
                                                                cart[item.id] =
                                                                    (cart[item.id]
                                                                        ?: 0) + 1
                                                            }
                                                        ) {
                                                            Text("+")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        "Total: R${money(cartTotal)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = {
                                            val order = JSONObject()

                                            order.put(
                                                "orderId",
                                                System.currentTimeMillis()
                                            )
                                            order.put("customer", currentEmail)
                                            order.put("status", "Pending")
                                            order.put("total", cartTotal)

                                            val lines = JSONArray()

                                            cartItems.forEach { item ->
                                                val line = JSONObject()

                                                line.put("itemId", item.id)
                                                line.put("name", item.name)
                                                line.put(
                                                    "quantity",
                                                    cart[item.id] ?: 0
                                                )
                                                line.put("price", item.price)

                                                lines.put(line)
                                            }

                                            order.put("items", lines)

                                            val orders =
                                                JSONArray(savedOrders)

                                            orders.put(order)
                                            saveOrders(orders.toString())

                                            cart.clear()
                                            selectedTab = "Orders"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Place order")
                                    }
                                }
                            }

                            "Orders" -> {
                                Text(
                                    "Order History",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                val orders = remember(savedOrders) {
                                    runCatching {
                                        JSONArray(savedOrders)
                                    }.getOrElse {
                                        JSONArray()
                                    }
                                }

                                if (orders.length() == 0) {
                                    Text(
                                        "You have not placed any orders yet."
                                    )
                                } else {
                                    LazyColumn(
                                        verticalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(orders.length()) { index ->
                                            val order =
                                                orders.getJSONObject(index)

                                            val orderId =
                                                order.optLong("orderId")

                                            val status =
                                                order.optString(
                                                    "status",
                                                    "Pending"
                                                )

                                            Card(
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(12.dp)
                                                ) {
                                                    Text(
                                                        "Order #$orderId",
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    )

                                                    Text(
                                                        "Total: R${
                                                            money(
                                                                order.optDouble(
                                                                    "total"
                                                                )
                                                            )
                                                        }"
                                                    )

                                                    Text("Status: $status")

                                                    val orderLines =
                                                        order.optJSONArray(
                                                            "items"
                                                        ) ?: JSONArray()

                                                    for (
                                                    i in 0 until
                                                            orderLines.length()
                                                    ) {
                                                        val line =
                                                            orderLines
                                                                .getJSONObject(i)

                                                        Text(
                                                            "${line.optString("name")} × ${line.optInt("quantity")}"
                                                        )
                                                    }

                                                    Spacer(
                                                        Modifier.height(8.dp)
                                                    )

                                                    Text(
                                                        "Demo status controls (local only)",
                                                        fontSize = 12.sp
                                                    )

                                                    Row(
                                                        horizontalArrangement =
                                                            Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                order.put(
                                                                    "status",
                                                                    "Confirmed"
                                                                )

                                                                saveOrders(
                                                                    orders.toString()
                                                                )
                                                            }
                                                        ) {
                                                            Text(
                                                                "Confirm",
                                                                fontSize = 10.sp
                                                            )
                                                        }

                                                        Button(
                                                            onClick = {
                                                                order.put(
                                                                    "status",
                                                                    "Delivered"
                                                                )

                                                                saveOrders(
                                                                    orders.toString()
                                                                )
                                                            }
                                                        ) {
                                                            Text(
                                                                "Delivered",
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Settings" -> {
                                Text(
                                    "Settings",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(12.dp))

                                Text("Signed in as $currentEmail")

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = darkMode,
                                        onCheckedChange = {
                                            darkMode = it

                                            preferences.edit()
                                                .putBoolean("dark_mode", it)
                                                .apply()
                                        }
                                    )

                                    Text("Dark mode")
                                }

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = notifications,
                                        onCheckedChange = {
                                            notifications = it

                                            preferences.edit()
                                                .putBoolean(
                                                    "notifications",
                                                    it
                                                )
                                                .apply()
                                        }
                                    )

                                    Text("Enable notifications preference")
                                }

                                Text(
                                    "The notification setting is saved locally; " +
                                            "push notifications are not implemented."
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = { logout() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Log out")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: FishItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Text(item.description)

            Text(
                "R${money(item.price)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quantity > 0) {
                    Button(onClick = onRemove) {
                        Text("−")
                    }

                    Spacer(Modifier.width(8.dp))

                    Text("$quantity")

                    Spacer(Modifier.width(8.dp))
                }

                Button(onClick = onAdd) {
                    Text("Add to cart")
                }
            }
        }
    }
}

// Supabase login and registration screen.
@Composable
private fun LoginScreen(
    preferences: android.content.SharedPreferences,
    onLogin: (AuthSession) -> Unit
) {
    val scope = rememberCoroutineScope()

    var isRegistering by remember {
        mutableStateOf(false)
    }

    var fullName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var busy by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "KwaRhadebe's Fisheries",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isRegistering) {
                "Create your account"
            } else {
                "Log in to your account"
            }
        )

        Spacer(Modifier.height(20.dp))

        if (isRegistering) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = !busy,
            onClick = {
                val cleanEmail = email.trim()

                if (cleanEmail.isBlank() || password.isBlank()) {
                    message = "Enter your email and password."
                } else if (
                    isRegistering && fullName.isBlank()
                ) {
                    message = "Enter your full name."
                } else if (password.length < 6) {
                    message =
                        "Use a password with at least 6 characters."
                } else {
                    scope.launch {
                        busy = true
                        message = ""

                        try {
                            if (isRegistering) {
                                val session =
                                    SupabaseAuthRepository.register(
                                        fullName = fullName.trim(),
                                        email = cleanEmail,
                                        password = password
                                    )

                                if (session != null) {
                                    onLogin(session)
                                } else {
                                    message =
                                        "Account created. Check your email " +
                                                "to confirm it, then log in."

                                    isRegistering = false
                                    password = ""
                                }
                            } else {
                                val session =
                                    SupabaseAuthRepository.login(
                                        email = cleanEmail,
                                        password = password
                                    )

                                onLogin(session)
                            }
                        } catch (e: Exception) {
                            message = e.message
                                ?: "Authentication failed. Please try again."
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (busy) {
                    "Please wait..."
                } else if (isRegistering) {
                    "Register"
                } else {
                    "Log in"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = !busy,
            onClick = {
                isRegistering = !isRegistering
                message = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegistering) {
                    "Already registered? Log in"
                } else {
                    "Create a new account"
                }
            )
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Kept for compatibility with any existing code that uses it.
// The new login screen no longer uses local password hashing.
private fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray(Charsets.UTF_8))

    return bytes.joinToString("") {
        "%02x".format(it)
    }
}

private fun money(amount: Double): String =
    String.format(Locale.US, "%.2f", amount)
