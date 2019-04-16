package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class CatalogSuccess(
    @SerializedName("catalog")
    val catalog: List<CatalogItem?>?,
    @SerializedName("sort_variants")
    val sortVariants: List<SortVariant?>?
)