package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class QuickOrder(
    @SerializedName("success")
    var success: String?,
    @SerializedName("error")
    var error: Error
)
