package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class BonusSuccess(
    @SerializedName("bonus")
    val bonus: Int?
)