package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Info(
    @SerializedName("success")
    val success: InfoSuccess?,
    @SerializedName("error")
    val error: Error?
)