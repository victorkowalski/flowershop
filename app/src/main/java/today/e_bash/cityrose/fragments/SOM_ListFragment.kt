package today.e_bash.cityrose.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_shops_on_map_list.*
import kotlinx.android.synthetic.main.som_list_item.view.*
import net.cachapa.expandablelayout.ExpandableLayout
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.R
import today.e_bash.cityrose.model.Addresses
import today.e_bash.cityrose.model.AddressesSuccess
import today.e_bash.cityrose.tools.EBashApi


class SOM_ListFragment : Fragment() {

    private val api = EBashApi.create()
    private val compositeDisposable = CompositeDisposable()

    class AddressListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val expandButton: LinearLayout? = view.expand_button
        val name: TextView? = view.name
        val address: TextView? = view.address
        val expandableLinearLayout: ExpandableLayout? = view.expandable_layout
        val infoBlock: LinearLayout? = view.info_block
    }

    class AddressListAdapter : RecyclerView.Adapter<AddressListViewHolder>() {
        private val items = ArrayList<AddressesSuccess>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressListViewHolder {
            return AddressListViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.som_list_item,
                    parent,
                    false
                )
            )
        }

        fun clear() {
            this.items.clear()
        }

        fun addItem(item: AddressesSuccess) {
            this.items.add(item)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(viewHolder: AddressListViewHolder, position: Int) {
            try {
                viewHolder.expandButton?.onClick {
                    viewHolder.expandableLinearLayout?.toggle()
                }
                val item = items[position]
                viewHolder.name?.text = item.name
                viewHolder.name?.context?.let { ctx ->
                    viewHolder.name.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_medium)
                }

                viewHolder.address?.text = item.address

                viewHolder.infoBlock?.removeAllViews()
                viewHolder.infoBlock?.verticalLayout {

                    textView {
                        text = "Телефон"
                        textColor = Color.parseColor("#787878")
                        textSize = 12f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(0)
                    }

                    textView {
                        text = item.phone
                        textColor = Color.BLACK
                        textSize = 14f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(3)
                    }

                    textView {
                        text = "Режим работы"
                        textColor = Color.parseColor("#787878")
                        textSize = 12f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(18)
                    }

                    textView {
                        text = item.workingHours
                        textColor = Color.BLACK
                        textSize = 14f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(3)
                    }

                    button {
                        text = resources.getString(R.string.build_a_route)
                        textSize = 12f
                        textColor = Color.WHITE
                        typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_bold)
                        background = resources.getDrawable(R.drawable.button_background, null)
                        onClick {
                            val intent = Intent(
                                android.content.Intent.ACTION_VIEW,
                                Uri.parse("google.navigation:q=${item.latitude},${item.longitude}")
                            )
                            this@button.context.startActivity(intent)
                        }
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(40)
                    }

                    leftPadding = dip(35)
                    rightPadding = dip(35)
                    topPadding = dip(26)
                    bottomPadding = dip(31)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun loadAddresses() {
        try {
            val accessToken =
                Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token in storage")

            api.addresses(accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Addresses> {
                    override fun onComplete() {

                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(addresses: Addresses) {
                        when {
                            addresses.success != null -> {
                                val adapter = recycler_view?.adapter as? AddressListAdapter
                                adapter?.clear()
                                addresses.success.forEach { point ->
                                    if (point != null) {
                                        adapter?.addItem(point)
                                    }
                                }
                                adapter?.notifyDataSetChanged()
                            }
                            addresses.error != null -> {
                                toast(addresses.error.message.toString()).show()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {

                    }

                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val divider = DividerItemDecoration(
                recycler_view?.context,
                DividerItemDecoration.VERTICAL
            )
            divider.setDrawable(resources.getDrawable(R.drawable.divider, null))
            recycler_view?.addItemDecoration(divider)
            recycler_view?.layoutManager = LinearLayoutManager(recycler_view?.context)
            recycler_view?.adapter = AddressListAdapter()

            loadAddresses()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shops_on_map_list, container, false)
    }
}