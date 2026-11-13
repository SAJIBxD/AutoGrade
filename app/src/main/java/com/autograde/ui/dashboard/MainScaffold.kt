package com.autograde.ui.dashboard

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.autograde.BottomNavItem
import com.autograde.R
import com.autograde.ui.Routes
import com.autograde.ui.theme.Accent
import com.autograde.ui.theme.AccentSoft
import com.autograde.ui.theme.Muted

/** The four tabs shown along the bottom of the signed-in part of the app. */
val mainTabs = listOf(
    BottomNavItem("Home", Routes.DASHBOARD, R.drawable.ic_tab_home),
    BottomNavItem("Assessments", Routes.EXAMS, R.drawable.ic_tab_assessments),
    BottomNavItem("Results", Routes.REPORTS, R.drawable.ic_tab_results),
    BottomNavItem("Profile", Routes.PROFILE, R.drawable.ic_tab_profile),
)

/**
 * Bottom-tab navigation: keeps one entry per tab on the back stack instead of
 * piling up a new entry on every tap, and remembers each tab's state.
 */
fun NavController.navigateToTab(route: String) {
    val current = currentDestination ?: return
    if (current.route == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The single bottom bar for the signed-in shell. It is hosted once by the app
 * scaffold (not per screen), so it never re-creates or flickers on tab switches.
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = mainTabs,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.route == currentRoute,
                onClick = { onItemClick(item) },
                icon = {
                    val icon = @Composable {
                        Icon(painterResource(item.iconRes), contentDescription = item.name)
                    }
                    if (item.badgeCount > 0) {
                        BadgedBox(badge = { Badge { Text(item.badgeCount.toString()) } }) { icon() }
                    } else {
                        icon()
                    }
                },
                label = { Text(item.name) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    indicatorColor = AccentSoft,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}
