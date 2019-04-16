package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

class Checkout {

    data class Response(
        @SerializedName("success")
        val success: String?,
        @SerializedName("error")
        val error: String?
    )
}