package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class SortVariant(
    @SerializedName("title")
    val title: String?,
    @SerializedName("url_parameters")
    val urlParameters: String?
)