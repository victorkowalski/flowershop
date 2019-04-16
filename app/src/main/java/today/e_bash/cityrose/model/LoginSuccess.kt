package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class LoginSuccess(
    @SerializedName("customer_id")
    val customerId: String?,
    @SerializedName("getPossibleMethods")
    val getPossibleMethods: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("token")
    val token: String?
)