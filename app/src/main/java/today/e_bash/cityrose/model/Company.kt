package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Company(
    @SerializedName("success")
    val success: String?,
    @SerializedName("error")
    val error: Error?
)