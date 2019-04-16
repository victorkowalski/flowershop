package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class InfoSuccess(
    @SerializedName("agreement")
    val agreement: String?,
    @SerializedName("birthday")
    val birthday: String?,
    @SerializedName("gender")
    val gender: String?,
    @SerializedName("lastName")
    val lastName: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("sureName")
    val sureName: String?
)