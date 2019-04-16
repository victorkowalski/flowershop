package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Login(
    @SerializedName("success")
    val success: LoginSuccess?,
    @SerializedName("error")
    val error: Error?
)