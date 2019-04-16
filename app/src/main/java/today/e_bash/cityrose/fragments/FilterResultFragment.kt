package today.e_bash.cityrose.fragments

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxItemDecoration
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_filter_result.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.textColor
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Product
import today.e_bash.cityrose.model.Products
import today.e_bash.cityrose.tools.EBashApi
import today.e_bash.cityrose.tools.EndlessRecyclerViewScrollListener

class FilterResultFragment : EFragment() {

    private val api = EBashApi.create()
    val token = Paper.book().read<String>("accessToken") ?: ""
    private val compositeDisposable = CompositeDisposable()
    private var filters = arrayListOf<FilterFragment.FilterParam>()
    private var filtersMap = hashMapOf<String, ArrayList<String>>()

    private fun getFiltersParam(): String {
        val result = ArrayList<String>()
        filtersMap.forEach {
            result.add("${it.key}:${it.value.joinToString(",")}")
        }
        return result.joinToString(";")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        try {
            val regex = Regex("^(.*?):(.*?)\$")
            val rawFilters = arguments?.getString("filters")
            val typeToken = object : TypeToken<List<FilterFragment.FilterParam>>() {}
            this.filters = Gson().fromJson(rawFilters, typeToken.type)
            filters.forEach { filter ->
                val matches = regex.find(filter.id ?: "")
                if ((matches?.groups?.size ?: 0) == 3) {
                    val g = matches?.groups?.get(1)?.value
                    val v = matches?.groups?.get(2)?.value
                    if (g != null && v != null) {
                        if (!filtersMap.containsKey(g)) {
                            filtersMap[g] = ArrayList()
                        }
                        filtersMap[g]?.add(v)
                    }
                }
            }
            println(getFiltersParam())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inflater.inflate(R.layout.fragment_filter_result, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }

    private fun loadProducts(page: Int) {
        try {
            progress_indicator?.visibility = View.VISIBLE
            api.products(filter_ocfilter = getFiltersParam(), page = page.toString(), accessToken = token)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Products> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: Products) {
                        progress_indicator?.visibility = View.GONE
                        if (t.success?.products == null) return
                        val adapter = (recycler_view?.adapter as? ProductsAdapter)
                        adapter?.addItems(t.success.products)
                        adapter?.notifyDataSetChanged()
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView? = view.findViewById(R.id.title)
        val image: ImageView? = view.findViewById(R.id.previewImage)
        val price: TextView? = view.findViewById(R.id.price)
        val discount: TextView? = view.findViewById(R.id.discount)
    }

    class ProductsAdapter : RecyclerView.Adapter<ProductViewHolder>() {
        private val products = arrayListOf<Product>()

        fun addItems(products: ArrayList<Product>) {
            this.products.addAll(products)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            return ProductViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_full_catalog_product,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return products.size
        }

        override fun onBindViewHolder(p: ProductViewHolder, position: Int) {
            try {
                val item = products[position]


                p.title?.text = item.title
                p.title?.textColor = Color.parseColor("#000000")
                val priceStringFormat = p.itemView.context?.getString(R.string.price).toString()
                when {
                    item.discount?.toFloat() == item.price?.toFloat() || item.discount?.toFloat() == 0F -> {
                        p.price?.textColor = Color.parseColor("#000000")
                        p.price?.text = String.format(priceStringFormat, item.price)
                        p.price?.context?.let { ctx ->
                            p.price.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                        }
                        p.price?.visibility = View.VISIBLE
                        p.discount?.visibility = View.GONE
                    }
                    item.discount?.toFloat()?.compareTo(item.price?.toFloat() ?: 0f) ?: 0 < 0 -> {
                        p.price?.visibility = View.VISIBLE
                        p.discount?.visibility = View.VISIBLE

                        p.discount?.textColor = Color.parseColor("#000000")
                        p.discount?.text = String.format(priceStringFormat, item.discount)
                        p.discount?.context?.let { ctx ->
                            p.discount.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
                        }
                        p.price?.textColor = Color.parseColor("#C4C4C4")
                        p.price?.text = String.format(priceStringFormat, item.price)
                        p.price?.paintFlags = p.price?.paintFlags?.or(Paint.STRIKE_THRU_TEXT_FLAG)!!
                    }
                    else -> {
                        p.price?.visibility = View.GONE
                        p.discount?.visibility = View.GONE
                    }
                }

                p.image?.let {
                    it.scaleType = ImageView.ScaleType.FIT_XY
                    Glide.with(it).load(item.image).into(it)
                }

                p.itemView.onClick {
                    try {
                        val args = Bundle()
                        args.putSerializable("product", Gson().toJson(item))
                        p.itemView.findNavController()
                            .navigate(R.id.action_filterResultFragment_to_productFragment, args)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val layoutManager = FlexboxLayoutManager(recycler_view?.context, FlexDirection.ROW, FlexWrap.WRAP)
            val flexboxItemDecoration = FlexboxItemDecoration(recycler_view?.context)
            flexboxItemDecoration.setDrawable(resources.getDrawable(R.drawable.full_catalog_item_divider, null))

            recycler_view?.addOnScrollListener(object : EndlessRecyclerViewScrollListener(layoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    loadProducts(page + 1)
                }
            })

            recycler_view?.layoutManager = layoutManager
            recycler_view?.addItemDecoration(flexboxItemDecoration)
            recycler_view?.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = dip(16)
                }
            })
            recycler_view?.adapter = ProductsAdapter()
            /*recycler_view?.addOnScrollListener(object : EndlessRecyclerViewScrollListener(layoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    loadProducts(page + 1)
                }
            })*/


            loadProducts(1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}