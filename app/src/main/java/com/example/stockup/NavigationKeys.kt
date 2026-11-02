package com.example.stockup

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Welcome : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object Main : NavKey
@Serializable data class AddProduct(val productId: Long? = null) : NavKey
@Serializable data class ProductDetail(val productId: Long) : NavKey
@Serializable data object LocationsManagement : NavKey
@Serializable data object Notifications : NavKey
@Serializable data object Faq : NavKey
