package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class PromoSuccess(
    @SerializedName("image")
    val image: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("original_height")
    val originalHeight: Int?,
    @SerializedName("original_width")
    val originalWidth: Int?,
    @SerializedName("screen_height")
    val screenHeight: Int?,
    @SerializedName("screen_width")
    val screenWidth: Int?,
    @SerializedName("title")
    val title: String?
)