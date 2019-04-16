package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Products(
    @SerializedName("success")
    val success: ProductsSuccess?,
    @SerializedName("error")
    val error: Error?
)