package today.e_bash.cityrose.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_promo.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Promo
import today.e_bash.cityrose.model.PromoSuccess
import today.e_bash.cityrose.tools.EBashApi

class PromoFragment : EFragment() {

    private val compositeDisposable = CompositeDisposable()
    private val api = EBashApi.create()

    class PromoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView? = view.findViewById(R.id.image)
    }

    class PromoAdapter : RecyclerView.Adapter<PromoViewHolder>() {
        private val items = ArrayList<PromoSuccess>()

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): PromoViewHolder {
            return PromoViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_promo, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(p: PromoViewHolder, position: Int) {

            val item = items[position]

            p.itemView.onClick {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                startActivity(p.itemView.context, browserIntent, null)
            }

            p.image?.let {
                it.scaleType = ImageView.ScaleType.FIT_XY
                Glide.with(it).load(item.image).into(it)
            }

            p.image?.layoutParams?.height = items[position].screenHeight
            p.image?.requestLayout()
        }

        fun addItem(item: PromoSuccess) {
            items.add(item)
            notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.promotions)
        return inflater.inflate(today.e_bash.cityrose.R.layout.fragment_promo, container, false)
    }

    private fun loadData() {
        try {
            val accessToken = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            val screenWidth = context?.resources?.displayMetrics?.widthPixels ?: 0
            api.promo(screenWidth, accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Promo> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(promo: Promo) {
                        when {
                            promo.success != null -> {
                                val adapter = recycler_view?.adapter as? PromoAdapter
                                promo.success.forEach { item ->
                                    if (item != null) {
                                        adapter?.addItem(item)
                                    }
                                }
                            }
                            promo.error != null -> {
                                toast(promo.error.message.toString()).show()
                            }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.orientation = LinearLayout.VERTICAL
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = PromoAdapter()

        loadData()
    }
}