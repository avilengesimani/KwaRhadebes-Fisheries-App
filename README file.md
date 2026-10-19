KwaRhadebe's Fisheries 🐟
KwaRhadebe's Fisheries is an Android application built with Kotlin and Jetpack Compose. It allows customers to browse a seafood menu, search for meals, manage a shopping cart, place orders, and view their order history.
The application uses Supabase for customer authentication and menu retrieval, while local Android storage is used for preferences and order history.
Features
1. Customer Authentication
•	Register an account using an email address and password.
•	Log in using an existing account.
•	Authenticate customers through Supabase Auth.
•	Save the login state and customer details locally.
•	Log out and clear the saved login session.
2. Menu
•	Retrieve available menu items from Supabase.
•	Display a demo menu when menu retrieval fails or returns no items.
•	Search for menu items by name or description.
•	Filter menu items by category:
o	All
o	Meals
o	Seafood
o	Sides
o	Drinks
3. Shopping Cart
•	Add menu items to the cart.
•	Increase or decrease item quantities.
•	Automatically calculate the cart total.
•	Display prices in South African rand (ZAR).
•	Place orders using the items currently in the cart.
4. Order History
•	Save placed orders locally on the device.
•	View order IDs, items, quantities, totals, and order statuses.
•	Change demo order statuses to Confirmed or Delivered.
Order history is currently stored locally using SharedPreferences and is not synchronised with Supabase.
5. Settings
•	Enable or disable dark mode.
•	Save the notification preference locally.
•	View the email address associated with the signed-in account.
•	Log out of the application.
The notification setting is a saved preference only; push notifications are not implemented.
Technologies Used
•	Kotlin — Android development.
•	Jetpack Compose — User interface.
•	Material 3 — UI components and theming.
•	Supabase Auth — Customer registration and login.
•	Supabase REST API — Retrieving menu items.
•	HTTPURLConnection — HTTP communication with Supabase.
•	Kotlin Coroutines — Asynchronous operations.
•	SharedPreferences — Local settings, login state, and demo order storage.
•	JSON — Processing API responses and local order data.
Project Structure
The application package is:
za.co.kwarhadebe.fisheries
File	Description
MainActivity.kt	Main screen, navigation, login UI, menu, cart, order history, and settings
SupabaseConfig.kt	Supabase project URL and publishable API key configuration
SupabaseMenuRepository.kt	Retrieves available menu items from Supabase
SupabaseAuthRepository.kt	Handles Supabase customer registration and login
build.gradle.kts	Android dependencies and build configuration
Requirements
To run the application, you need:
•	Android Studio.
•	A compatible Android SDK and JDK.
•	A Supabase account and project.
•	An Android emulator or physical Android device.
•	An internet connection for Supabase authentication and menu retrieval.
Supabase Configuration
1. Create a Supabase project
Visit the Supabase Dashboard and create a project.
2. Configure the project connection
Open SupabaseConfig.kt and configure the project URL and publishable API key.
The configuration follows this structure:
package za.co.kwarhadebe.fisheries

object SupabaseConfig {
    const val PROJECT_URL = "https://YOUR_PROJECT_REF.supabase.co"
    const val PUBLISHABLE_KEY = "YOUR_SUPABASE_PUBLISHABLE_KEY"
}
Replace the example values with your actual Supabase project details.
Never include a service-role key or other privileged secret in your Android application.
3. Enable email authentication
In your Supabase Dashboard:
1.	Open Authentication.
2.	Go to the sign-in providers.
3.	Enable email authentication.
4.	Review the email confirmation settings.
Depending on your configuration, customers may need to confirm their email addresses before logging in.
4. Configure the menu table
The menu repository retrieves records from public.menu_items.
The table must contain the following columns:
Column	Description
item_id	Unique numeric menu item ID
item_name	Name of the menu item
description	Description of the item
price	Item price
category	Menu category
available	Indicates whether the item is available
The application requests available items and sorts them by item_id.
Configure appropriate Row Level Security (RLS) policies to control access to the menu data.
How to Run the Application
1.	Open the project in Android Studio.
2.	Confirm that the Supabase configuration and repository files are present.
3.	Enter your Supabase project URL and publishable key.
4.	Ensure that your Supabase authentication provider and menu table are configured.
5.	Sync the project with Gradle.
6.	Run the app on an emulator or Android device.
How to Use the Application
Register or log in
1.	Open the application.
2.	Enter your email address and password.
3.	Select Create a new account if you are registering.
4.	Enter your full name and complete registration.
5.	If email confirmation is required, confirm your email before logging in.
6.	Log in to access the main application.
Browse the menu
1.	Open the Menu tab.
2.	Search by menu item name or description.
3.	Select a category to filter the displayed items.
4.	Add items to your cart.
Manage your cart
1.	Open the Cart tab.
2.	Use the plus and minus buttons to change quantities.
3.	Review the calculated total.
4.	Tap Place order to save the order locally and open Order History.
View your orders
1.	Open the Orders tab.
2.	Review your saved orders and their details.
3.	Use the demo status controls to mark orders as Confirmed or Delivered.
These order status changes are local to the device.
Change settings
1.	Open the Settings tab.
2.	Toggle dark mode as desired.
3.	Change the notification preference.
4.	Tap Log out when finished.
Data Storage
Information	Where it is stored
Customer account and authentication	Supabase Auth
Menu items	Supabase database
Login state and customer details	Android SharedPreferences
Dark mode preference	Android SharedPreferences
Notification preference	Android SharedPreferences
Order history	Android SharedPreferences
The application currently saves order history locally. Orders are not yet stored in Supabase.
Security Notes
•	Use the Supabase publishable key for client-side access.
•	Never expose privileged Supabase secrets in the application.
•	Use appropriate Row Level Security policies for database access.
•	Treat locally stored login state as a convenience, not proof of a valid server-side session.
•	Customer order data stored locally is not automatically backed up or synchronised across devices.
Project Status
KwaRhadebe's Fisheries is a working Android application with customer authentication, Supabase menu retrieval, shopping cart functionality, local order history, and application settings.
________________________________________
Project: KwaRhadebe's Fisheries
Platform: Android
Language: Kotlin
UI Framework: Jetpack Compose
Backend: Supabase

