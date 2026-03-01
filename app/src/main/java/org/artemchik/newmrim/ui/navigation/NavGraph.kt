package org.artemchik.newmrim.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.artemchik.newmrim.ui.screens.ChatScreen
import org.artemchik.newmrim.ui.screens.ContactListScreen
import org.artemchik.newmrim.ui.screens.LoginScreen

object Routes {
    const val LOGIN = "login"; const val CONTACTS = "contacts"; const val CHAT = "chat/{email}"
    fun chat(email: String) = "chat/$email"
}

@Composable
fun MrimNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(onLoginSuccess = { navController.navigate(Routes.CONTACTS) { popUpTo(Routes.LOGIN) { inclusive = true } } }) }
        composable(Routes.CONTACTS) { ContactListScreen(onContactClick = { navController.navigate(Routes.chat(it)) }, onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }) }
        composable(route = Routes.CHAT, arguments = listOf(navArgument("email") { type = NavType.StringType })) { ChatScreen(onBack = { navController.popBackStack() }) }
    }
}
