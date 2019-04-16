package today.e_bash.cityrose.fragments


import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.text.Layout
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_oneclick_order.*
import kotlinx.android.synthetic.main.fragment_product.*
import kotlinx.android.synthetic.main.product_option.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.find
import ss.com.bannerslider.adapters.SliderAdapter
import ss.com.bannerslider.viewholder.ImageSlideViewHolder
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.events.UpdateCart
import today.e_bash.cityrose.model.Cart
import today.e_bash.cityrose.model.CatalogProduct
import today.e_bash.cityrose.model.Info
import today.e_bash.cityrose.model.QuickOrder
import today.e_bash.cityrose.tools.EBashApi
import java.security.InvalidParameterException


class ProductFragment : EFragment() {

    private var product: CatalogProduct? = null
    private val compositeDisposable = CompositeDisposable()
    private var productId: String? = null
    private var info: Info? = null

    data class ProductExParam(
        @SerializedName("title") val title: String,
        @SerializedName("data") val data: String
    )

    data class ProductExOption(
        @SerializedName("product_option_id") val product_option_id: String,
        @SerializedName("name") val name: String,
        @SerializedName("price") val price: String,
        @SerializedName("extra") val extra: String,
        @SerializedName("discount") val discount: String,
        @SerializedName("product_option_value_id") val product_option_value_id: String,
        @SerializedName("url_parameters") val url_parameters: String,
        var selected: Boolean? = false
    )

    data class ProductItemEx(
        @SerializedName("id") val id: String,
        @SerializedName("code") val code: String,
        @SerializedName("title") val title: String,
        @SerializedName("extra") val extra: String,
        @SerializedName("discount") val discount: String,
        @SerializedName("price") val price: String,
        @SerializedName("inStock") val inStock: String,
        @SerializedName("description") val description: String,
        @SerializedName("params") val params: ArrayList<ProductExParam>,
        @SerializedName("images") val images: ArrayList<String>,
        @SerializedName("url_parameters") val url_parameters: String,
        @SerializedName("options") val options: ArrayList<ProductExOption>
    )

    class OptionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radioButton: RadioButton? = view.title
    }

    class OptionsAdapter(private val options: ArrayList<ProductExOption>) : RecyclerView.Adapter<OptionsViewHolder>() {
        interface IOptionListener {
            fun optionSelect(price: String, discount: String, extra: String)
        }

        private var optionSelectListener: IOptionListener? = null

        fun setOptionSelectListener(listener: (String, String, String) -> Unit) {
            this.optionSelectListener = object : IOptionListener {
                override fun optionSelect(price: String, discount: String, extra: String) {
                    listener(price, discount, extra)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): OptionsViewHolder {
            return OptionsViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.product_option,
                    parent,
                    false
                )
            )
        }

        init {
            if (getSelectedOption() == null && options.size > 0) {
                options[0].selected = true
            }
        }

        private fun onClick(position: Int) {
            try {
                val item = options[position]
                optionSelectListener?.optionSelect(item.price, item.discount, item.extra)
                for (option in options) {
                    option.selected = false
                }
                options[position].selected = true
                notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getSelectedOption(): ProductExOption? {
            for (option in options) {
                if (option.selected == true) {
                    return option
                }
            }
            return null
        }

        override fun getItemCount(): Int {
            return options.size
        }

        override fun onBindViewHolder(p: OptionsViewHolder, position: Int) {
            p.radioButton?.onClick { onClick(position) }
            when (options[position].selected) {
                true -> p.radioButton?.isChecked = true
                false -> p.radioButton?.isChecked = false
                else -> p.radioButton?.isChecked = false
            }
            p.radioButton?.text = options[position].name
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.product)

        val jsonProduct: String = arguments?.get("product")?.toString() ?: throw InvalidParameterException()
        this.product = Gson().fromJson(jsonProduct, CatalogProduct::class.java)
        product?.let {
            return inflater.inflate(R.layout.fragment_product, container, false)
        } ?: run {
            activity?.onBackPressed()
            return null
        }
    }

    private fun addProductToCart(id: String, button: Button?) {
        try {
            val progressDialog = AlertDialog.Builder(button?.context)
                .setView(R.layout.progress_dialog)
                .create()

            progressDialog.show()

            button?.isEnabled = false
            val api = EBashApi.create()
            val accessToken =
                Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            val option =
                (find<RecyclerView>(R.id.options_recycler_view).adapter as? OptionsAdapter)?.getSelectedOption()

            val options = mutableMapOf<String, String>()
            option?.let { o ->
                options.put(String.format("option[%s]", o.product_option_id), o.product_option_value_id)
            }

            api.cartAdd(id, "1", options, accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Cart.CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: Cart.CartResponse) {
                        if (t.success == null) return
                        EventBus.getDefault().post(UpdateCart(t.success.goods.size, t.success.totalPrice ?: ""))
                        try {
                            progressDialog.dismiss()
                            button?.isEnabled = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun orderOneClick(ctx: Context) {
        try {
            val dialog = AlertDialog.Builder(ctx).create()
            val view = ctx.layoutInflater.inflate(R.layout.fragment_oneclick_order, null)

            val titleTv = view.findViewById<TextView?>(R.id.title)
            val nameTv = view.findViewById<EditText?>(R.id.name)
            val phoneTv = view.findViewById<EditText?>(R.id.phone)

            titleTv?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
            nameTv?.setText(info?.success?.name)
            phoneTv?.setText(info?.success?.phone)

            val submitButton = view.findViewById<Button?>(R.id.submit)
            submitButton?.onClick {
                try {
                    val name = nameTv?.text?.toString() ?: ""
                    val phone = phoneTv?.text?.toString() ?: ""

                    dialog.setCancelable(false)
                    dialog.setContentView(ctx.frameLayout {
                        progressBar {
                            isIndeterminate = true
                        }.lparams {
                            gravity = Gravity.CENTER
                            topMargin = dip(20)
                            bottomMargin = dip(20)
                        }

                    })
                    dialog.show()

                    val option =
                        (find<RecyclerView>(R.id.options_recycler_view).adapter as? OptionsAdapter)?.getSelectedOption()

                    val options = mutableMapOf<String, String>()
                    option?.let { o ->
                        options.put(String.format("option[%s]", o.product_option_id), o.product_option_value_id)
                    }

                    val api = EBashApi.create()
                    val accessToken =
                        Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")

                    api.quickOrder(
                        this@ProductFragment.productId ?: "",
                        options, "1", name, phone, accessToken
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : Observer<QuickOrder> {
                            override fun onComplete() {
                            }

                            override fun onSubscribe(d: Disposable) {
                                compositeDisposable.add(d)
                            }

                            override fun onNext(t: QuickOrder) {
                                if (t.success == null) return
                                dialog.setCancelable(true)
                                val successMessageView =
                                    LayoutInflater.from(ctx).inflate(R.layout.quick_order_success_message, null)

                                val messageTv = successMessageView.findViewById<TextView?>(R.id.message)
                                messageTv?.text = t.success

                                val continueButton = successMessageView?.findViewById<Button?>(R.id.continue_button)
                                continueButton?.onClick {
                                    dialog.dismiss()
                                }
                                dialog.setContentView(successMessageView)
                                /*dialog.setContentView(ctx.verticalLayout {
                                    padding = dip(15)
                                    textView {
                                        text = t.success
                                    }
                                })*/

                            }

                            override fun onError(e: Throwable) {
                                EventBus.getDefault().post(ErrorEvent(e))
                            }
                        })

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            dialog.setView(view)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun renderProductEx(p: ProductItemEx) {
        //ui thread
        try {
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels

            when {
                root_view != null && root_view.context != null -> {
                    root_view.removeAllViews()
                    val bannerSlider = ss.com.bannerslider.Slider(root_view.context)

                    when (resources.configuration.orientation) {
                        Configuration.ORIENTATION_PORTRAIT ->
                            bannerSlider.layoutParams = LinearLayout.LayoutParams(width, width)
                        Configuration.ORIENTATION_LANDSCAPE ->
                            bannerSlider.layoutParams = LinearLayout.LayoutParams(height, height / 2)
                        else -> bannerSlider.layoutParams = LinearLayout.LayoutParams(0, 0)
                    }

                    (bannerSlider.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.CENTER_HORIZONTAL

                    bannerSlider.setAdapter(object : SliderAdapter() {
                        override fun getItemCount(): Int {
                            return p.images.size
                        }

                        override fun onBindImageSlide(position: Int, imageSlideViewHolder: ImageSlideViewHolder?) {
                            imageSlideViewHolder?.bindImageSlide(p.images[position])
                        }
                    })

                    root_view?.verticalLayout {
                        if (p.images.isNotEmpty()) {
                            addView(bannerSlider)
                        }

                        verticalLayout {
                            textView {
                                text = p.title
                                textSize = 16F
                                typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_bold)
                                textColor = Color.BLACK
                            }.lparams {
                                topMargin = dip(15)
                            }

                            textView {
                                id = R.id.extra
                                text = p.extra
                                textSize = 12F
                            }

                            textView {
                                text = resources.getString(R.string.select_size)
                                textSize = 12F
                                typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_medium)
                                textColor = Color.BLACK
                                visibility = if (p.options.isEmpty()) View.GONE else View.VISIBLE
                            }.lparams(width = wrapContent, height = wrapContent) { topMargin = dip(1) }

                            recyclerView {
                                id = R.id.options_recycler_view
                                layoutManager = FlexboxLayoutManager(this.context, FlexDirection.ROW, FlexWrap.WRAP)
                                adapter = OptionsAdapter(p.options)
                            }.lparams(width = matchParent, height = wrapContent) { }

                            linearLayout {
                                orientation = LinearLayout.HORIZONTAL
                                val price = p.price.toFloatOrNull()
                                val discount = p.discount.toFloatOrNull()

                                val haveDiscount =
                                    discount != null && price != null && discount > 0F && discount < price
                                val priceFormat = resources.getString(R.string.price)

                                textView {
                                    id = R.id.discount
                                    visibility = if (haveDiscount) View.VISIBLE else View.GONE
                                    text = String.format(priceFormat, p.discount)
                                    textSize = 16F
                                    textColor = Color.parseColor("#000000")
                                    typeface = if (haveDiscount)
                                        ResourcesCompat.getFont(this.context, R.font.montserrat_semibold)
                                    else
                                        ResourcesCompat.getFont(this.context, R.font.montserrat)

                                }.lparams(width = wrapContent, height = wrapContent) {}

                                textView {
                                    id = R.id.price
                                    text = String.format(priceFormat, p.price)
                                    textSize = 16F
                                    textColor = if (haveDiscount) Color.parseColor("#C4C4C4") else Color.BLACK
                                    paintFlags =
                                        if (haveDiscount)
                                            this.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
                                        else
                                            this.paintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv())

                                    typeface = if (haveDiscount)
                                        ResourcesCompat.getFont(this.context, R.font.montserrat)
                                    else
                                        ResourcesCompat.getFont(this.context, R.font.montserrat_semibold)

                                }.lparams(width = wrapContent, height = wrapContent) { leftMargin = dip(5) }
                            }

                            textView {
                                text = p.inStock
                                textSize = 12F
                                typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_regular)
                            }.lparams(width = wrapContent, height = wrapContent) { topMargin = dip(1) }


                            button {
                                background = resources.getDrawable(R.drawable.button_background, null)
                                text = "Добавить в корзину"
                                textColor = Color.WHITE
                                allCaps = false
                                textSize = 12F
                                typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_bold)
                                onClick {
                                    addProductToCart(p.id, this@button)
                                }
                            }.lparams(width = matchParent, height = wrapContent) {
                                topMargin = dip(10)
                            }

                            button {
                                background = resources.getDrawable(R.drawable.button_background_invert, null)
                                text = "Купить в 1 клик"
                                allCaps = false
                                textSize = 12F
                                typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_bold)
                                onClick {
                                    orderOneClick(this@button.context)
                                }
                            }.lparams(width = matchParent, height = wrapContent) {
                                topMargin = dip(10)
                            }

                            textView {
                                text = p.description
                                textSize = 12F
                                textColor = Color.BLACK
                            }.lparams {
                                topMargin = dip(24)
                            }

                            view {
                                backgroundColor = Color.parseColor("#EAEAEA")
                            }.lparams(width = matchParent, height = dip(1)) {
                                topMargin = dip(20)
                                bottomMargin = dip(20)
                            }

                            p.params.forEach { param ->
                                val pView = LayoutInflater.from(this.context).inflate(R.layout.product_param_item, null)
                                pView?.findViewById<TextView?>(R.id.title)?.text = param.title
                                pView?.findViewById<TextView?>(R.id.value)?.text = param.data
                                val layoutParams = ConstraintLayout.LayoutParams(
                                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.bottomMargin = dip(15)
                                pView?.layoutParams = layoutParams
                                addView(pView)
                            }

                            textView {
                                text = String.format("Code %s", p.code)
                                textColor = Color.RED
                            }
                        }.lparams(width = matchParent, height = wrapContent) {
                            leftMargin = dip(30)
                            rightMargin = dip(30)
                        }
                    }
                }
            }

            val optionsAdapter = (find<RecyclerView>(R.id.options_recycler_view).adapter as? OptionsAdapter)
            optionsAdapter?.setOptionSelectListener { priceString, discountString, extra ->

                try {
                    val price = priceString.toFloatOrNull()
                    val discount = discountString.toFloatOrNull()

                    val haveDiscount = discount != null && price != null && discount > 0F && discount < price
                    val priceFormat = resources.getString(R.string.price)

                    val priceView = root_view?.findViewById<TextView?>(R.id.price)
                    val discountView = root_view?.findViewById<TextView?>(R.id.discount)

                    if (priceView != null && discountView != null) {
                        priceView.text = String.format(priceFormat, priceString)
                        priceView.paintFlags =
                            if (haveDiscount)
                                priceView.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
                            else
                                priceView.paintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv())

                        priceView.typeface =
                            if (haveDiscount)
                                ResourcesCompat.getFont(priceView.context, R.font.montserrat)
                            else
                                ResourcesCompat.getFont(priceView.context, R.font.montserrat_semibold)


                        discountView.text = String.format(priceFormat, discountString)
                        discountView.visibility = if (haveDiscount) View.VISIBLE else View.GONE
                        discountView.typeface =
                            ResourcesCompat.getFont(discountView.context, R.font.montserrat_semibold)
                    }

                    val extraView = root_view?.findViewById<TextView?>(R.id.extra)
                    extraView?.text = extra

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doAsync {
            try {
                val product = this@ProductFragment.product ?: throw InvalidParameterException()
                val api = EBashApi.create()
                val accessToken =
                    Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")

                api.info(accessToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Info> {
                        override fun onComplete() {
                        }

                        override fun onSubscribe(d: Disposable) {
                            compositeDisposable.add(d)
                        }

                        override fun onNext(t: Info) {
                            if (t.success == null) return
                            this@ProductFragment.info = t
                        }

                        override fun onError(e: Throwable) {
                            EventBus.getDefault().post(ErrorEvent(e))
                        }
                    })

                val response = api.product(product.id ?: "", accessToken).execute().body()
                    ?: throw Exception("Invalid response")
                when {
                    response.has("error") -> printError(response)
                    response.has("success") -> {
                        val successObject = response.getAsJsonObject("success")
                        val productEx = Gson().fromJson(successObject, ProductItemEx::class.java)
                        productEx?.let {
                            this@ProductFragment.productId = productEx.id
                            uiThread {
                                renderProductEx(productEx)
                            }
                        }
                    }
                    else -> throw Exception("Unknown response type")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}