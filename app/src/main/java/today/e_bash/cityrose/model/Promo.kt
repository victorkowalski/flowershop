package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Promo(
    @SerializedName("success")
    val success: List<PromoSuccess?>?,
    @SerializedName("error")
    val error: Error?
)