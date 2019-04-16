package today.e_bash.cityrose.fragments

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_cart.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.events.UpdateCart
import today.e_bash.cityrose.model.Cart.*
import today.e_bash.cityrose.tools.EBashApi

class CartFragment : EFragment() {

    private val TAG = "CartFragment"
    private val compositeDisposable = CompositeDisposable()
    private val api = EBashApi.create()
    private val token = Paper.book().read<String>("accessToken") ?: "null"

    class CartProductItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView? = view.findViewById(R.id.title)
        var discount: TextView? = view.findViewById(R.id.discount)
        var price: TextView? = view.findViewById(R.id.price)
        var previewImage: ImageView? = view.findViewById(R.id.previewImage)
        var removeButton: ImageView? = view.findViewById(R.id.removeItemButton)
        var progressBar: ProgressBar? = view.findViewById(R.id.progressBar)
        var add1Item: ImageView? = view.findViewById(R.id.add1Item)
        var remove1Item: ImageView? = view.findViewById(R.id.remove1Item)
        var countTextEdit: EditText? = view.findViewById(R.id.countTextEdit)
        var remove1ItemProgress: ProgressBar? = view.findViewById(R.id.remove1ItemProgress)
        var add1ItemProgress: ProgressBar? = view.findViewById(R.id.add1ItemProgress)
    }

    class AdditionalItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView? = view.findViewById(R.id.title)
        val addButton: ImageView? = view.findViewById(R.id.addButton)
        val progressBar: ProgressBar? = view.findViewById(R.id.progressBar)
        val previewImage: ImageView? = view.findViewById(R.id.imageView)
        val discount: TextView? = view.findViewById(R.id.discount)
        val price: TextView? = view.findViewById(R.id.price)
    }

    interface ICart {
        fun removeItem(id: String) {}
        fun addItem(id: String) {}
        fun updateCart(result: CartResponse)
        fun openProductCart(item: String) {}
    }

    class CartProductsAdapter : RecyclerView.Adapter<CartProductItemViewHolder>() {

        private val cartItems = ArrayList<CartProduct>()
        private var cartInterface: ICart? = null


        fun setCartInterface(cartInterface: ICart) {
            this.cartInterface = cartInterface
        }

        fun clear() {
            cartItems.clear()
        }

        fun addItems(items: List<CartProduct>) {
            cartItems.addAll(items)
        }

        private fun changeItemCount(key: String, newValue: String) {
            try {
                val token = Paper.book().read<String>("accessToken") ?: ""
                val api = EBashApi.create()
                val compositeDisposable = CompositeDisposable()

                api.cartEdit(key, newValue, token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<CartResponse> {
                        override fun onComplete() {
                        }

                        override fun onSubscribe(d: Disposable) {
                            compositeDisposable.add(d)
                        }

                        override fun onNext(t: CartResponse) {
                            if (t.success == null) return
                            if (cartInterface == null) return
                            cartInterface?.updateCart(t)
                        }

                        override fun onError(e: Throwable) {
                            EventBus.getDefault().post(ErrorEvent(e))
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartProductItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false)
            return CartProductItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return cartItems.size
        }

        override fun onBindViewHolder(p: CartProductItemViewHolder, position: Int) {
            val item = cartItems[position]
            //if (item.quantity?.toInt() ?: 1 > 1) "${item.title} (${item.quantity})" else item.title
            p.title?.text = item.title
            p.previewImage?.let {
                Glide.with(it.context).load(item.image).into(it)
            }

            val priceStringFormat = p.itemView.context?.getString(R.string.price).toString()

            val discountPaintFlags = p.discount?.paintFlags ?: 0
            val pricePaintFlags = p.price?.paintFlags ?: 0

            p.discount?.paintFlags = (discountPaintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv()))
            p.price?.paintFlags = (pricePaintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv()))

            when {
                item.discount?.toFloat() == item.price?.toFloat() || item.discount?.toFloat() == 0F -> {
                    p.price?.textColor = Color.parseColor("#000000")
                    p.price?.text = String.format(priceStringFormat, item.price)
                    p.price?.visibility = View.VISIBLE
                    p.discount?.visibility = View.GONE
                    p.price?.context?.let { ctx ->
                        p.price?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                    }
                }
                item.discount?.toFloat()?.compareTo(item.price?.toFloat() ?: 0f) ?: 0 < 0 -> {
                    p.price?.textColor = Color.parseColor("#C4C4C4")
                    p.discount?.textColor = Color.parseColor("#000000")
                    p.discount?.text = String.format(priceStringFormat, item.discount)
                    p.price?.text = String.format(priceStringFormat, item.price)
                    p.price?.paintFlags = pricePaintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
                    p.discount?.context?.let { ctx ->
                        p.discount?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                    }
                    p.price?.visibility = View.VISIBLE
                    p.discount?.visibility = View.VISIBLE
                }
                else -> {
                    p.price?.visibility = View.GONE
                    p.discount?.visibility = View.GONE
                }
            }

            p.removeButton?.visibility = View.VISIBLE
            p.progressBar?.visibility = View.GONE


            p.remove1Item?.visibility = View.VISIBLE
            p.remove1ItemProgress?.visibility = View.GONE
            p.remove1Item?.onClick {
                val oldCount = p.countTextEdit?.text.toString().toInt()
                if (oldCount > 1) {
                    val newCount = oldCount - 1
                    changeItemCount(item.id ?: "", newCount.toString())
                    //p.countTextEdit?.setText(newCount.toString())
                }
                p.remove1Item?.visibility = View.GONE
                p.remove1ItemProgress?.visibility = View.VISIBLE
            }

            p.add1Item?.visibility = View.VISIBLE
            p.add1ItemProgress?.visibility = View.GONE
            p.add1Item?.onClick {
                val oldCount = p.countTextEdit?.text.toString().toInt()
                val newCount = oldCount + 1
                //p.countTextEdit?.setText(newCount.toString())
                changeItemCount(item.id ?: "", newCount.toString())
                p.add1Item?.visibility = View.GONE
                p.add1ItemProgress?.visibility = View.VISIBLE
            }

            p.countTextEdit?.setText((item.quantity?.toInt() ?: 1).toString())

            p.removeButton?.onClick {
                p.removeButton?.visibility = View.GONE
                p.progressBar?.visibility = View.VISIBLE
                if (item.id != null) {
                    cartInterface?.removeItem(item.id)
                }
            }
        }
    }

    class AdditionalAdapter : RecyclerView.Adapter<AdditionalItemViewHolder>() {

        private val additionalItemItems = ArrayList<AdditionalProduct>()

        private var cartInterface: ICart? = null


        fun setCartInterface(cartInterface: ICart) {
            this.cartInterface = cartInterface
        }

        fun clear() {
            additionalItemItems.clear()
            this.notifyDataSetChanged()
        }

        fun addItems(items: List<AdditionalProduct>) {
            additionalItemItems.addAll(items)
            this.notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdditionalItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.additional_item, parent, false)
            return AdditionalItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return additionalItemItems.size
        }

        override fun onBindViewHolder(p: AdditionalItemViewHolder, position: Int) {
            try {
                val item = additionalItemItems[position]
                p.progressBar?.visibility = View.GONE
                p.addButton?.visibility = View.VISIBLE

                p.previewImage?.let {
                    Glide.with(it.context).load(item.image).into(it)
                }

                try {

                    p.itemView.onClick {
                        cartInterface?.openProductCart(Gson().toJson(item))
                    }

                    val priceStringFormat = p.itemView.context?.getString(R.string.price).toString()

                    val discountPaintFlags = p.discount?.paintFlags ?: 0
                    val pricePaintFlags = p.price?.paintFlags ?: 0

                    p.discount?.paintFlags = (discountPaintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv()))
                    p.price?.paintFlags = (pricePaintFlags.and(Paint.STRIKE_THRU_TEXT_FLAG.inv()))

                    when {
                        item.discount?.toFloat() == item.price?.toFloat() || item.discount?.toFloat() == 0F -> {
                            p.price?.textColor = Color.parseColor("#000000")
                            p.price?.text = String.format(priceStringFormat, item.price)
                            p.price?.visibility = View.VISIBLE
                            p.discount?.visibility = View.GONE
                            p.price?.context?.let { ctx ->
                                p.price.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                            }
                        }
                        item.discount?.toFloat()?.compareTo(item.price?.toFloat() ?: 0f) ?: 0 < 0 -> {
                            p.price?.textColor = Color.parseColor("#C4C4C4")
                            p.discount?.textColor = Color.parseColor("#000000")
                            p.discount?.text = String.format(priceStringFormat, item.discount)
                            p.price?.text = String.format(priceStringFormat, item.price)
                            p.price?.paintFlags = pricePaintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
                            p.discount?.context?.let { ctx ->
                                p.discount.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                            }
                            p.price?.visibility = View.VISIBLE
                            p.discount?.visibility = View.VISIBLE
                        }
                        else -> {
                            p.price?.visibility = View.GONE
                            p.discount?.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    p.price?.visibility = View.GONE
                    p.discount?.visibility = View.GONE
                    e.printStackTrace()
                }

                p.title?.text = item.title
                p.addButton?.onClick {
                    p.progressBar?.visibility = View.VISIBLE
                    p.addButton.visibility = View.GONE
                    if (item.id != null) {
                        cartInterface?.addItem(item.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.basket)
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    private fun renderCartItems(result: CartSuccess) {
        if (products_recycler_view?.adapter is CartProductsAdapter) {
            val adapter = products_recycler_view?.adapter as? CartProductsAdapter
            adapter?.clear()
            adapter?.addItems(result.goods)
            adapter?.notifyDataSetChanged()
        }
    }

    private fun renderAdditionalItems(result: CartSuccess) {
        if (additional_recycler_view?.adapter is AdditionalAdapter) {

            rb_all?.text = "Все"
            rb_filtered?.text = result.additionals?.get(1)?.title ?: ""

            val adapter = additional_recycler_view?.adapter as? AdditionalAdapter
            adapter?.clear()
            result.additionals?.forEach {
                val products = it.products?.filterNotNull()
                if (products != null)
                    adapter?.addItems(products)
            }
            adapter?.notifyDataSetChanged()

            rb_additional_group?.onCheckedChange { _, checkedId ->
                when (checkedId) {
                    R.id.rb_all -> {
                        adapter?.clear()
                        result.additionals?.forEach {
                            val products = it.products?.filterNotNull()
                            if (products != null)
                                adapter?.addItems(products)
                        }
                        adapter?.notifyDataSetChanged()
                    }
                    R.id.rb_filtered -> {
                        adapter?.clear()
                        if (result.additionals?.size ?: 0 > 1) {
                            val filteredProducts = result.additionals?.get(1)?.products?.filterNotNull()
                            if (filteredProducts != null) {
                                adapter?.addItems(filteredProducts)
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun renderAll(t: CartResponse) {
        try {
            if (t.success == null) return
            val priceStringFormat = this@CartFragment.context?.getString(R.string.price).toString()
            order_price?.text = String.format(priceStringFormat, t.success.totalPrice)
            order_price?.context?.let { ctx ->
                order_price?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
            }
            renderCartItems(t.success)
            renderAdditionalItems(t.success)
            progress_indicator?.visibility = View.GONE
            scroll_view?.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeItem(id: String): Boolean {
        /*try {
            val api = EBashApi.create()
            val token = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            val response = api.cartRemove(id, token).execute().body() ?: throw Exception("Invalid response")
            when {
                response.has("error") -> printError(response)
                response.has("success") -> {
                    return true
                }
                else -> throw Exception("Unknown response type")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/

        try {
            api.cartRemove(id, token)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: CartResponse) {
                        if (t.success == null) return
                        EventBus.getDefault().post(UpdateCart(t.success.goods.size, t.success.totalPrice ?: ""))
                        renderAll(t)
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun addItem(id: String): Boolean {
        try {
            api.cartAdd(id, "1", emptyMap(), token)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Observer<CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: CartResponse) {
                        if (t.success == null) return
                        renderAll(t)
                        EventBus.getDefault().post(UpdateCart(t.success.goods.size, t.success.totalPrice ?: ""))
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun updateCart() {
        try {
            api.cart(token)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: CartResponse) {
                        if (t.success == null) return
                        try {
                            renderAll(t)
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

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        additional_products?.context?.let { ctx ->
            additional_products?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
        }

        val divider = DividerItemDecoration(
            this@CartFragment.context,
            DividerItemDecoration.VERTICAL
        )
        divider.setDrawable(resources.getDrawable(R.drawable.cart_divider, null))
        products_recycler_view?.addItemDecoration(divider)
        additional_recycler_view?.addItemDecoration(divider)

        val cartAdapter = CartProductsAdapter()
        cartAdapter.setCartInterface(object : ICart {
            override fun removeItem(id: String) {
                doAsync {
                    if (this@CartFragment.removeItem(id)) {
                        updateCart()
                    }
                }
            }

            override fun updateCart(result: CartResponse) {
                renderAll(result)
            }
        })
        products_recycler_view.adapter = cartAdapter
        products_recycler_view.layoutManager = LinearLayoutManager(view.context)

        val additionalAdapter = AdditionalAdapter()
        additionalAdapter.setCartInterface(object : ICart {
            override fun addItem(id: String) {
                doAsync {
                    if (this@CartFragment.addItem(id)) {
                        updateCart()
                    }
                }
            }

            override fun updateCart(result: CartResponse) {
                renderAll(result)
            }

            override fun openProductCart(item: String) {
                val args = Bundle()
                args.putString("product", item)
                findNavController().navigate(R.id.action_cartFragment_to_productFragment, args)
            }
        })
        additional_recycler_view.adapter = additionalAdapter
        additional_recycler_view.layoutManager = LinearLayoutManager(view.context)

        checkout?.onClick {
            findNavController().navigate(R.id.action_cartFragment_to_orderFragment)
        }

        updateCart()
    }
}