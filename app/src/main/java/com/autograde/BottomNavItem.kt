package com.autograde

import androidx.annotation.DrawableRes

data class BottomNavItem(
    val name: String,
    val route: String,
    @DrawableRes val iconRes: Int,
    val badgeCount: Int = 0,
)
