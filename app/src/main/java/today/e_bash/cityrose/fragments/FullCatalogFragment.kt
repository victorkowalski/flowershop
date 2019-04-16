package today.e_bash.cityrose.fragments

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
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
import com.google.gson.annotations.SerializedName
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_full_catalog.*
import kotlinx.android.synthetic.main.item_full_catalog_product.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.CatalogItem
import today.e_bash.cityrose.model.CatalogProduct
import today.e_bash.cityrose.model.Product
import today.e_bash.cityrose.model.Products
import today.e_bash.cityrose.tools.EBashApi
import java.security.InvalidParameterException

class FullCatalogFragment : EFragment() {

    private val TAG = "FullCatalogFragment"
    private var catalogItem: CatalogItem? = null
    private var api = EBashApi.create()
    private var token = Paper.book().read<String>("accessToken") ?: ""
    private val compositeDisposable = CompositeDisposable()

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView? = view.previewImage
        val title: TextView? = view.title
        val price: TextView? = view.price
        val discount: TextView? = view.discount
    }

    class ProductsAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val products = ArrayList<Product>()

        fun addProducts(items: ArrayList<Product>) {
            products.addAll(items)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ProductViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_full_catalog_product, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return products.size
        }

        override fun onBindViewHolder(p: RecyclerView.ViewHolder, position: Int) {

            if (p is ProductViewHolder) {
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
                                .navigate(R.id.action_fullCatalogFragment_to_productFragment, args)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.catalog)

        try {
            //inflater.inflate(R.layout.fragment_full_catalog, container, false)
            catalogItem = Gson().fromJson(
                arguments?.get("catalogItem").toString(),
                CatalogItem::class.java
            )

            catalogItem?.let {
                return inflater.inflate(R.layout.fragment_full_catalog, container, false)
            } ?: run {
                activity?.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    /*
    private fun getProducts(): ArrayList<CatalogProduct> {
        val result = ArrayList<CatalogProduct>()
        try {
            val api = EBashApi.create()
            val token = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            val response = api.products(catalogItem?.path ?: "", token).execute().body()
                ?: throw Exception("Invalid response")
            when {
                response.has("error") -> printError(response)
                response.has("success") -> {
                    val productResponse = Gson().fromJson(
                        response.getAsJsonObject("success"),
                        ProductsResponse::class.java
                    ) ?: throw InvalidParameterException()
                    result.addAll(productResponse.products)
                }
                else -> throw Exception("Unknown response type")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun showProducts(products: ArrayList<CatalogProduct>) {
        Log.d(TAG, products.toString())
        progress_indicator?.visibility = View.GONE
        /*when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> recycler_view?.layoutManager = GridLayoutManager(activity, 2)
            Configuration.ORIENTATION_LANDSCAPE -> recycler_view?.layoutManager = GridLayoutManager(activity, 4)
            else -> recycler_view?.layoutManager = GridLayoutManager(activity, 2)
        }*/
        val flexboxLayoutManager = FlexboxLayoutManager(recycler_view?.context, FlexDirection.ROW, FlexWrap.WRAP)
        val flexboxItemDecoration = FlexboxItemDecoration(recycler_view?.context)
        flexboxItemDecoration.setDrawable(resources.getDrawable(R.drawable.full_catalog_item_divider, null))
        recycler_view?.layoutManager = flexboxLayoutManager
        recycler_view?.addItemDecoration(flexboxItemDecoration)
        recycler_view?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = dip(16)
            }
        })
        recycler_view?.adapter = ProductsAdapter(products)
        recycler_view?.adapter?.notifyDataSetChanged()
    }*/

    private fun loadProducts(page: Int) {
        api.products(page = page.toString(), accessToken = token, filter_ocfilter = catalogItem?.filterOcfilter)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(object : Observer<Products> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onNext(t: Products) {
                    if (t.success?.products == null) return
                    (recycler_view?.adapter as? ProductsAdapter)?.addProducts(t.success.products)
                    (recycler_view?.adapter as? ProductsAdapter)?.notifyDataSetChanged()
                    progress_indicator?.visibility = View.GONE

                }

                override fun onError(e: Throwable) {
                    EventBus.getDefault().post(ErrorEvent(e))
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val flexboxLayoutManager = FlexboxLayoutManager(recycler_view?.context, FlexDirection.ROW, FlexWrap.WRAP)
            val flexboxItemDecoration = FlexboxItemDecoration(recycler_view?.context)
            flexboxItemDecoration.setDrawable(resources.getDrawable(R.drawable.full_catalog_item_divider, null))
            recycler_view?.layoutManager = flexboxLayoutManager
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
            recycler_view?.adapter?.notifyDataSetChanged()

            loadProducts(1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*doAsync {
            val products = getProducts()
            uiThread {
                showProducts(products)
            }
        }*/
    }
}