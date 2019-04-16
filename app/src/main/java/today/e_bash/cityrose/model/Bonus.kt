package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Bonus(
    @SerializedName("success")
    val success: BonusSuccess?,
    @SerializedName("error")
    val error: Error?
)