package com.example.supos

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey

@Serializable data object Main : NavKey

@Serializable data object OrderHistory : NavKey

@Serializable data object Performance : NavKey

@Serializable data object OnlineOrderReceiver : NavKey
