package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

class Cart {

    data class AdditionalProduct(
        val id: String?,
        val title: String?,
        val price: String?,
        val discount: String?,
        val type: String?,
        val image: String?
    )

    data class Additional(
        val title: String?,
        val products: List<AdditionalProduct?>?
    )

    data class CartProduct(
        val id: String?,
        val product_id: String?,
        val title: String?,
        val quantity: String?,
        val price: String?,
        val discount: String?,
        val image: String?,
        val total: String?
    )

    data class CartSuccess(
        val goods: List<CartProduct>,
        val additionals: List<Additional>?,
        val totalPrice: String?
    )

    data class CartResponse(
        @SerializedName("success")
        val success: CartSuccess?,
        @SerializedName("error")
        val error: Error?
    )
}