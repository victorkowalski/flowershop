package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class CatalogItem(
    @SerializedName("description")
    val description: String?,
    @SerializedName("filter_ocfilter")
    val filterOcfilter: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("limit")
    val limit: Int?,
    @SerializedName("order")
    val order: String?,
    @SerializedName("path")
    val path: String?,
    @SerializedName("products")
    val products: List<CatalogProduct?>?,
    @SerializedName("sort")
    val sort: String?,
    @SerializedName("title")
    val title: String?
)