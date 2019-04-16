package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class Addresses(
    @SerializedName("success")
    val success: List<AddressesSuccess?>?,
    @SerializedName("error")
    val error: Error?
)