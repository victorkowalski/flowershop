package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Error(
    @SerializedName("error")
    val error: Int?,
    @SerializedName("message")
    val message: String?
)