package com.example.supos.data.network

import com.google.gson.annotations.SerializedName

data class MenuResponsePayload(
    @SerializedName("success") val success: Boolean,
    @SerializedName("menu") val menu: List<MenuItemPayload>,
    @SerializedName("options") val options: List<OptionItemPayload>?
)

data class MenuItemPayload(
    @SerializedName("id") val id: String, // ID might come as String or Int from GAS, using String to be safe
    @SerializedName("category") val category: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: String, // Same for price
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("options_group") val optionsGroup: String?
)

data class OptionItemPayload(
    @SerializedName("group") val group: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: String,
    @SerializedName("optionType") val optionType: String?
)
