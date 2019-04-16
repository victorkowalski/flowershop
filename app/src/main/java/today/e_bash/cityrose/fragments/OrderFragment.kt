package today.e_bash.cityrose.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import kotlinx.android.synthetic.main.fragment_order.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Info
import today.e_bash.cityrose.model.PossibleShippingTime
import today.e_bash.cityrose.tools.EBashApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_cart.*
import org.jetbrains.anko.sdk27.coroutines.textChangedListener
import org.jetbrains.anko.support.v4.toast
import ru.yandex.money.android.sdk.*
import today.e_bash.cityrose.BuildConfig
import today.e_bash.cityrose.model.Bonus
import today.e_bash.cityrose.model.Cart
import java.math.BigDecimal
import today.e_bash.cityrose.model.Checkout
//import ru.yandex.money.android.sdk.Checkout

class OrderFragment : EFragment() {

    val RUB = Currency.getInstance("RUB")
    val REQUEST_CODE_TOKENIZE = 33

    private var phone: String = ""
    private var name: String = ""

    private var api = EBashApi.create()
    private var token = Paper.book().read<String>("accessToken") ?: ""
    private var compositeDisposable = CompositeDisposable()
    private var possibleShippingTime = ArrayList<String>()
    private var cartResponse: Cart.CartResponse? = null

    private var bonuses: Int = 0
    private var orderAmount: Int = 0
    private var bonusesAvailable: Int = 0
    private var writeOffBonuses: Int = 0
    private var deliveryAmount: Int = 0
    private var orderTotal: Int = 0
    private var exactDeliveryTime = 0

    private fun getInfo() {
        try {
            api.info(token)
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
                        this@OrderFragment.name = t.success.name ?: ""
                        this@OrderFragment.phone = t.success.phone ?: ""
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.order)
        this.getInfo()
        api.getPossibleShippingTime(token)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<PossibleShippingTime> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onNext(t: PossibleShippingTime) {
                    if (t.success == null) return
                    possibleShippingTime.clear()
                    possibleShippingTime.addAll(t.success)
                    println(possibleShippingTime)
                    /*val adapter = ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_dropdown_item_1line, possibleShippingTime
                    )*/
                    val ctx = this@OrderFragment.context
                    if (ctx != null) {
                        val adapter = ArrayAdapter<String>(
                            ctx, android.R.layout.simple_dropdown_item_1line, possibleShippingTime
                        )
                        time_of_delivery?.setAdapter(adapter)
                    }
                }

                override fun onError(e: Throwable) {
                    EventBus.getDefault().post(ErrorEvent(e))
                }
            })

        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    private fun setupExactDeliveryTime(){
        var hours = arrayListOf<String>()
        for (a in 8..22) {
            var h = a.toString()
            hours.add("$h:00") //h + ":00"
            if (a != 22)
                hours.add("$h:30") //h + ":30"
        }

        val ctx = this@OrderFragment.context
        if (ctx != null) {
            val adapter = ArrayAdapter<String>(
                ctx, android.R.layout.simple_dropdown_item_1line, hours
            )
            exact_time_of_delivery?.setAdapter(adapter)
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cb_write_postcard.onCheckedChange { _, isChecked ->
            et_postcard.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        i_will_take_order_myself.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> {
                    recipient_name?.setText(this@OrderFragment.name)
                    if(this@OrderFragment.phone.length > 0)
                       recipient_phone?.setText(this@OrderFragment.phone.removeRange(0, 4)) // +7 (  is already there
                    from_whom_wrapper_all?.visibility = View.GONE
                }
                false -> {
                    recipient_name?.setText("")
                    recipient_phone?.setText("")
                    from_whom_wrapper_all?.visibility = View.VISIBLE
                }
            }
        }

        recipient_additional_info_cb?.onCheckedChange { _, isChecked ->
            when (isChecked) {
                true -> recipient_additional_info_et?.visibility = View.VISIBLE
                false -> recipient_additional_info_et?.visibility = View.GONE
            }
        }

        anonymous_order?.setOnCheckedChangeListener { _, isChecked ->
            from_whom_wrapper?.visibility = if (isChecked) View.GONE else View.VISIBLE
        }


        day_of_delivery?.onClick {
            try {
                val ctx = this@OrderFragment.context ?: throw Exception("ctx == nil")

                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val datePickerDialog = DatePickerDialog(
                    ctx,
                    DatePickerDialog.OnDateSetListener { _, y, m, d ->
                        calendar.set(Calendar.YEAR, y)
                        calendar.set(Calendar.MONTH, m)
                        calendar.set(Calendar.DAY_OF_MONTH, d)
                        val formatter = SimpleDateFormat("dd.MM.yyyy")
                        day_of_delivery?.setText(formatter.format(calendar.time))
                    }, year, month, day
                )
                datePickerDialog.datePicker.minDate = calendar.timeInMillis
                datePickerDialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        exact_delivery_time_cb.onCheckedChange { _, isChecked ->
            when (isChecked) {
                true -> {
                    //time_of_delivery_wrapper?.visibility = View.GONE
                    delivery_wrapper?.visibility = View.VISIBLE
                    time_of_delivery.visibility = View.GONE
                    exact_time_of_delivery.visibility = View.VISIBLE
                    deliveryAmount = 500  //todo: get from cart
                    calculate()
                }
                false -> {
                    //time_of_delivery_wrapper?.visibility = View.VISIBLE
                    delivery_wrapper?.visibility = View.GONE
                    time_of_delivery.visibility = View.VISIBLE
                    exact_time_of_delivery.visibility = View.GONE
                    deliveryAmount = 0
                    calculate()
                }
            }
        }

        cb_write_postcard_label?.onClick {
            cb_write_postcard?.isChecked = cb_write_postcard?.isChecked?.not() ?: false
        }

        btn_write_off_bonuses?.onClick {
            var writeOffBonusesStr = write_off_bonuses?.text.toString()
            if (writeOffBonusesStr.length == 0) {
                bonuses_wrapper?.visibility = View.GONE
                writeOffBonuses = 0
                calculate()
            } else {
                if (writeOffBonusesStr.toInt() > bonusesAvailable) {
                    showMsgBox(resources.getString(R.string.bonus_amount_exceeded))
                } else {
                    bonuses_wrapper?.visibility = View.VISIBLE
                    writeOffBonuses = writeOffBonusesStr.toInt()
                    calculate()
                }
            }
        }

/*
        write_off_bonuses?.textChangedListener {
            this.beforeTextChanged { s, start, count, after ->
                if (s.toString().toFloat() > bonuses ?: .0f) {

                }
            }
        }*/
/*
        RxTextView.textChanges(write_off_bonuses).skip(1).subscribe { it ->
            if (it.toString().length == 0) {
                writeOffBonuses = 0
                calculate()
            } else {
                if (it.toString().toInt() > bonusesAvailable) {
                    write_off_bonuses.error = resources.getString(R.string.bonus_amount_exceeded)

                } else {
                    writeOffBonuses = it.toString().toInt()
                    calculate()
                }
            }
        }
*/

        loadBonuses()
        getCart()

        setupBuy()
        setupExactDeliveryTime()

    }

    private fun showMsgBox(message: String){
        val alertDialog = AlertDialog
            .Builder(this@OrderFragment.context!!)
            .create()

        val view = LayoutInflater.from(this@OrderFragment.context)
            .inflate(R.layout.messagebox, null)
        view.findViewById<TextView>(R.id.message).text = message
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        continueButton.onClick {
            alertDialog.dismiss()
        }

        alertDialog.setView(view)
        alertDialog?.show()
    }

    private fun isValidFields(): Boolean{
        var recipientNameValid = true
        var recipientPhoneValid = true
        var dateValid = true
        var timeValid = true
        recipient_name.error = null
        recipient_phone.error = null
        day_of_delivery.error = null
        time_of_delivery.error = null

        var data = getOrderSendData()

        if(data?.recipient_name?.length!! == 0) {
            recipientNameValid = false
            recipient_name.error = resources.getString(R.string.name_required)
        }
        if(data?.recipient_phone?.length!! < 11) {
            recipientPhoneValid = false
            recipient_phone.error = resources.getString(R.string.phone_required)
        }
        if(data?.delivery_date?.length!! == 0) {
            dateValid = false
            day_of_delivery.error = resources.getString(R.string.date_required)
        }

        if (data?.delivery_exact_time == "1"){
            if(data?.delivery_exact_time_val?.length!! == 0) {
                timeValid = false
                exact_time_of_delivery.error = resources.getString(R.string.time_required)
            }
        }else{
            if(data?.delivery_time?.length!! == 0) {
                timeValid = false
                time_of_delivery.error = resources.getString(R.string.time_required)
            }
        }

        return (recipientNameValid
                && recipientPhoneValid
                && dateValid
                && timeValid
                )
    }

    private fun setupBuy() {
        buy?.onClick {

            if (isValidAmount() && isValidFields()) {
                if (rb_payment_type_group.checkedRadioButtonId == cash_payment.id) {
                    proceedCash()
                } else {
                    proceedSdk()
                }
            }else{
                toast(resources.getString(R.string.all_fields_required)).show()
            }
        }
    }

    private fun proceedCash(){
        checkout(null, "CASH")
    }

    private fun proceedSdk() {
        val paymentParameters = PaymentParameters(
            Amount(orderTotal.toBigDecimal()!!, RUB),
            resources.getString(R.string.yakassa_product_name),
            resources.getString(R.string.yakassa_product_description),
            BuildConfig.MERCHANT_TOKEN,
            BuildConfig.SHOP_ID,
            getPaymentMethodTypes()
        )

        val uiParameters = UiParameters()
        val testParameters = TestParameters(true, false)

        // Start MSDK to get payment token
        val context = this@OrderFragment.context ?: throw Exception("ctx == nil")
        val intent = ru.yandex.money.android.sdk.Checkout.createTokenizeIntent(
            context,
            paymentParameters,
            testParameters,
            uiParameters
        )
        startActivityForResult(intent, REQUEST_CODE_TOKENIZE)

    }

    private fun loadBonuses() {
        api.bonuses(token)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<Bonus> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onNext(t: Bonus) {
                    if (t.success == null) return
                    this@OrderFragment.bonuses = t.success.bonus!!
                    calculate()
                }

                override fun onError(e: Throwable) {
                    EventBus.getDefault().post(ErrorEvent(e))
                }
            })
    }

    private fun isValidAmount(): Boolean {
        var totalPrice = BigDecimal(cartResponse?.success?.totalPrice)
        return totalPrice.compareTo(BigDecimal.ZERO) > 0
    }

    private fun getPaymentMethodTypes(): Set<PaymentMethodType> {
        val paymentMethodTypes = HashSet<PaymentMethodType>()

        val checkedRadioButtonId: Int = rb_payment_type_group.checkedRadioButtonId
        when (checkedRadioButtonId) {
            rb_bank_card.id -> {
                paymentMethodTypes.add(PaymentMethodType.BANK_CARD)
            }
            /*rb_yamoney.id -> {
                paymentMethodTypes.add(PaymentMethodType.YANDEX_MONEY)
            }*/
        }
        return paymentMethodTypes
    }

    private fun getOrderDataMap(): MutableMap<String, String>{
        val order = getOrderSendData()
        val orderMap = mutableMapOf<String, String>()
        orderMap["name"] = order?.from_name!!
        orderMap["order_myself"] = order.order_myself!!
        orderMap["recipient_name"] = order.recipient_name!!
        orderMap["recipient_phone"] = order.recipient_phone!!
        orderMap["shipping_address_1"] = order.recipient_address!!
        orderMap["recipient_add_info"] = order.recipient_add_info!!
        orderMap["delivery_date"] = order.delivery_date!!
        orderMap["delivery_time"] = order.delivery_time!!
        orderMap["delivery_exact_time"] = order.delivery_exact_time!!
        orderMap["delivery_exact_time_val"] = order.delivery_exact_time_val!!
        orderMap["from_anon"] = order.from_anon!!
        //orderMap["distance"] = order.distance!!
        orderMap["from_name"] = order.from_name!!
        orderMap["from_phone"] = order.from_phone!!
        orderMap["from_email"] = order.from_email!!

        orderMap["reward"] = order.reward!!
        orderMap["amount"] = order.amount!!

        return orderMap
    }

    private fun checkout(paymentToken: String?, paymentType: String) {
        api.checkout(token, paymentToken, paymentType, getOrderDataMap())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<Checkout.Response> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onNext(resp: Checkout.Response) {
                    if (resp.success != null){
                        showMsgBox(resp.success)
                    } else {
                        showMsgBox(resp.error!!)
                    }

                    //toast("Oплата прошла успешно").show()
                }

                override fun onError(e: Throwable) {
                    EventBus.getDefault().post(ErrorEvent(e))
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Receive token from mSDK

        if (requestCode == REQUEST_CODE_TOKENIZE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val  result:TokenizationResult = ru.yandex.money.android.sdk.Checkout.createTokenizationResult(data!!)
                    var paymentToken: String = result.paymentToken
                    var paymentType: String = result.paymentMethodType.name

                    checkout(paymentToken, paymentType)
                    //Log.d("onActivityResult", "RESULT_CANCELED")
                    // successful tokenization
                    //TokenizationResult result = Checkout.createTokenizationResult(data);
                    /*startActivity(
                        SuccessTokenizeActivity.createIntent(
                            this, paymentToken, paymentMethodType.name
                        )
                    )*/
                }
                Activity.RESULT_CANCELED -> Log.d("onActivityResult", "RESULT_CANCELED")
                    // user canceled tokenization
                    //Toast.makeText(this?, R.string.auth_screen_message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCart() {
        try {
            api.cart(token)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Cart.CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(cartResponse: Cart.CartResponse) {
                        if (cartResponse.success == null) return
                        try {
                            this@OrderFragment.cartResponse = cartResponse

                            calculate()
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

    private fun renderAll() {
        try {
            if (cartResponse?.success == null) return
            val priceStringFormat = this@OrderFragment.context?.getString(R.string.price).toString()  // ???
            //order_amount?.text = String.format(priceStringFormat, cartResponse?.success?.totalPrice)
            order_amount?.text = String.format(priceStringFormat, orderAmount)
            order_amount?.context?.let { ctx ->
                order_amount?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
            }

            bonuses_available?.setText(bonusesAvailable.toString())

            val priceFormat = resources.getString(R.string.price)

            bonuses_count_label?.text = String.format(priceFormat, bonuses.toString())
            bonus_amount.text = String.format(resources.getString(R.string.write_off_bonuses), writeOffBonuses)
            delivery_amount.text = String.format(priceFormat, deliveryAmount.toString())

            order_total.text = String.format(priceFormat, orderTotal.toString())
            //renderCartItems(t.success)
            //renderAdditionalItems(t.success)
            //progress_indicator?.visibility = View.GONE
            //scroll_view?.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculate(){

        if(cartResponse == null) return
        orderAmount = cartResponse?.success?.totalPrice?.toInt()!!

        bonusesAvailable = orderAmount?.times(0.3)?.toInt()
        if(bonusesAvailable > bonuses)
            bonusesAvailable = bonuses
        //writeOffBonuses
        //deliveryAmount
        orderTotal = orderAmount.minus(writeOffBonuses).plus(deliveryAmount) //.plus(exactDeliveryTime)

        /*
        private var bonuses: Int? = null
        private var cartResponse: Cart.CartResponse? = null

        private var totalPrice: Double? = null
        private var bonusesAvailable: Int? = null
        private var writeOffBonuses: Int? = null
        private var orderTotal: Double? = null*/

        renderAll()
    }


    private fun getOrderSendData(): OrderSendData? {
        try {
            val result = OrderSendData()

            result.postcard_text = et_postcard?.text?.toString()

            result.order_myself = if (i_will_take_order_myself?.isChecked == true) "1" else "0"

            result.recipient_name = recipient_name?.text.toString()
            result.recipient_phone = recipient_phone?.text.toString()
            result.recipient_address = "${recipient_address?.text} офис: ${recipient_address_office?.text}"
            result.recipient_add_info = recipient_additional_info_et?.text?.toString()

            result.delivery_date = day_of_delivery?.text.toString()
            result.delivery_time = time_of_delivery?.text.toString()
            result.delivery_exact_time_val = exact_time_of_delivery?.text.toString()
            result.delivery_exact_time = if (exact_delivery_time_cb?.isChecked!!) "1" else "0"

            result.from_anon = if (anonymous_order?.isChecked == true) "1" else "0"
            result.from_name = from_name?.text.toString()
            result.from_phone = from_phone?.text.toString()
            result.from_email = from_email?.text.toString()

            result.reward = writeOffBonuses.toString()
            result.amount = orderTotal.toString()

            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    data class OrderSendData(
        var postcard_text: String? = null,

        var order_myself: String? = null,
        var recipient_name: String? = null,
        var recipient_phone: String? = null,
        var recipient_address: String? = null,
        var recipient_add_info: String? = null,

        var delivery_date: String? = null,
        var delivery_time: String? = null,
        var delivery_exact_time: String? = null,
        var delivery_exact_time_val: String? = null,

        var from_anon: String? = null,
        var from_name: String? = null,
        var from_phone: String? = null,
        var from_email: String? = null,

        //var write_off_bonus: String? = null,
        var reward: String? = null,

        var payment_type: String? = null,

        var amount: String? = null
    )
}