package today.e_bash.cityrose.model

import com.google.gson.annotations.SerializedName

data class AddressesSuccess(
    @SerializedName("address")
    val address: String?,
    @SerializedName("latitude")
    val latitude: String?,
    @SerializedName("longitude")
    val longitude: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("working_hours")
    val workingHours: String?
)