package today.e_bash.cityrose.tools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
import today.e_bash.cityrose.events.ApiError
import today.e_bash.cityrose.model.*


interface EBashApi {

    class EBashCookie : CookieJar {

        private var cookies: List<Cookie>? = null

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            cookies?.let {
                return it
            }
            return ArrayList()
        }
    }

    @GET("?route=api/mob_app&action=pin")
    fun getPin(
        @Query("phone") phone: String,
        @Query("test") test: String = "0"
    ): Observable<PIN>

    @GET("?route=api/mob_app&action=login")
    fun login(
        @Query("pin") pin: String,
        @Query("token") authToken: String
    ): Observable<Login>

    @GET("?route=api/mob_app&action=company")
    fun company(@Query("token") accessToken: String): Observable<Company>

    @GET("?route=api/mob_app&action=about")
    fun about(@Query("token") accessToken: String): Observable<About>

    @GET("?route=api/mob_app&action=promo")
    fun promo(
        @Query("screen_width") screenWidth: Int,
        @Query("token") accessToken: String
    ): Observable<Promo>

    @GET("?route=api/mob_app&action=bonuses")
    fun bonuses(@Query("token") accessToken: String): Observable<Bonus>

    @GET("?route=api/mob_app&action=info")
    fun info(@Query("token") accessToken: String): Observable<Info>

    @GET("?route=api/mob_app&action=update")
    fun update(
        @Query("name") name: String,
        @Query("lastName") lastName: String,
        @Query("sureName") sureName: String,
        @Query("phone") phone: String,
        @Query("agreement") agreement: String,
        @Query("gender") gender: String,
        @Query("birthday") birthday: String,
        @Query("token") accessToken: String
    ): Observable<Info>

    @GET("?route=api/mob_app&action=addresses")
    fun addresses(@Query("token") accessToken: String): Observable<Addresses>

    @GET("?route=api/mob_app&action=quick_order")
    fun quickOrder(
        @Query("product_id") product_id: String,
        @QueryMap options: Map<String, String>,
        @Query("quantity") quantity: String = "1",
        @Query("name") name: String,
        @Query("phone") phone: String,
        @Query("token") accessToken: String
    ): Observable<QuickOrder>

    @GET("?route=api/mob_app&action=catalog")
    fun catalog(@Query("token") accessToken: String): Observable<Catalog>

    @GET("?route=api/mob_app&action=cart")
    fun cart(@Query("token") accessToken: String): Observable<Cart.CartResponse>

    @GET("?route=api/mob_app&action=product")
    fun product(@Query("product_id") product_id: String, @Query("token") accessToken: String): Call<JsonObject>

    @GET("?route=api/mob_app&action=cart_add")
    fun cartAdd(
        @Query("product_id") product_id: String,
        @Query("quantity") quantity: String,
        @QueryMap options: Map<String, String>,
        @Query("token") accessToken: String
    ): Observable<Cart.CartResponse>

    @GET("?route=api/mob_app&action=cart_edit")
    fun cartEdit(
        @Query("key") key: String,
        @Query("quantity") quantity: String,
        @Query("token") accessToken: String
    ): Observable<Cart.CartResponse>

    @GET("?route=api/mob_app&action=cart_remove")
    fun cartRemove(
        @Query("key") key: String,
        @Query("token") accessToken: String
    ): Observable<Cart.CartResponse>

    @GET("?route=api/mob_app&action=products")
    fun products(
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("limit") limit: String? = "20",
        @Query("path") path: String? = "10",
        @Query("page") page: String? = "1",
        @Query("filter_ocfilter") filter_ocfilter: String? = null,
        @Query("token") accessToken: String?
    ): Observable<Products>

    @GET("?route=api/mob_app&action=products")
    fun products(
        @Query("path") path: String,
        @Query("token") accessToken: String
    ): Call<JsonObject>

    @GET("?route=api/mob_app&action=get_filter")
    fun getFilter(
        @Query("token") accessToken: String
    ): Call<JsonObject>

    @GET("?route=api/mob_app&action=get_possible_shipping_time")
    fun getPossibleShippingTime(
        @Query("token") accessToken: String
    ): Observable<PossibleShippingTime>

    @GET("?route=api/mob_app&action=checkout")
    fun checkout(
        @Query("token") accessToken: String,
        @Query("ya_kassa_token") yaKassaToken: String?,
        @Query("payment_type") paymentType: String,
        @QueryMap data: Map<String, String>
    ): Observable<Checkout.Response>

    /* companion object */
    companion object Factory {
        private const val baseUrl = "http://test.flowwill.ru/"
        private val cookieStore = EBashCookie()

        fun create(): EBashApi {
            val gson = GsonBuilder()
                .setLenient()
                .create()

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor {
                    val request = it.request()
                    val response = it.proceed(request)
                    try {
                        if (response.isSuccessful) {
                            val body = response.body()
                            val bodyString = response.body()?.string() ?: ""
                            val jsonObject = Gson().fromJson(bodyString, JsonElement::class.java).asJsonObject
                            if (jsonObject.has("error")) {
                                val errorJsonObject = jsonObject.getAsJsonObject("error")
                                val errorObject = Gson().fromJson(errorJsonObject, Error::class.java)
                                EventBus.getDefault().post(ApiError(errorObject))
                            }
                            return@addInterceptor response.newBuilder()
                                .body(ResponseBody.create(body?.contentType(), bodyString))
                                .build()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return@addInterceptor response
                }
                .cookieJar(cookieStore)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

            return retrofit.create(EBashApi::class.java)
        }
    }
}



