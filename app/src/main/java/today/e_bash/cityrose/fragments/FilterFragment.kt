package today.e_bash.cityrose.fragments

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.CheckBox
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import io.paperdb.Paper
import kotlinx.android.synthetic.main.filter_group.view.*
import kotlinx.android.synthetic.main.fragment_filter.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.tools.EBashApi
import java.util.concurrent.CopyOnWriteArrayList

class FilterFragment : EFragment() {

    data class FilterParam(
        val id: String?,
        val title: String?
    )

    data class Filter(
        val title: String?,
        val params: ArrayList<FilterParam>?,
        val multiple: Int?
    )

    data class FiltersResponse(
        val filters: ArrayList<Filter>?
    )

    private val selectedFilters = CopyOnWriteArrayList<FilterParam>()

    class FilterOptionVH(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view
        val title: TextView? = view.findViewById(R.id.title)
        val checkBox: CheckBox? = view.findViewById(R.id.checkbox)
    }

    class FilterOptionsAdapter(
        private val items: ArrayList<FilterParam>,
        private val selectedItems: CopyOnWriteArrayList<FilterParam>,
        private val s: IFilterOptions? = null
    ) :
        RecyclerView.Adapter<FilterOptionVH>() {

        interface IFilterOptions {
            fun itemSelected(item: FilterParam)
            fun itemUnselected(item: FilterParam)
            fun setSelectedCount(count: Int)
        }

        init {
            val selectedCount = items.filter { selectedItems.contains(it) }.count()
            s?.setSelectedCount(selectedCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterOptionVH {
            return FilterOptionVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.filter_item, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private fun shakeItBaby(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                (ctx.getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(2, 10))
            } else {
                @Suppress("DEPRECATION")
                (ctx.getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(2)
            }
        }

        private fun getViewsByTag(root: ViewGroup, tag: String?): ArrayList<View> {
            val views = ArrayList<View>()
            val childCount = root.childCount
            for (i in 0 until childCount) {
                val child = root.getChildAt(i)
                if (child is ViewGroup) {
                    views.addAll(getViewsByTag(child, tag))
                }

                val tagObj = child.tag
                if (tagObj != null && tagObj == tag) {
                    views.add(child)
                }

            }
            return views
        }

        private fun resetPrices(p: FilterOptionVH){
            var priceTag = "p"
            selectedItems.forEach { item ->
                val isPriceParam = checkNotNull(item.id).take(1).equals(priceTag)
                if(isPriceParam){
                    s?.itemUnselected(item)
                }
            }

            val parent = p.root.getParent()
            val pricesChBoxes = getViewsByTag(parent as ViewGroup, priceTag)
            val chbxCurrent = p.checkBox

            pricesChBoxes.forEach {
                val chbx = it as CheckBox
                if(chbxCurrent == chbx) {
                    return@forEach
                }
                chbx.isChecked = false
            }
        }

        override fun onBindViewHolder(p: FilterOptionVH, position: Int) {
            try {
                p.root.onClick {
                    shakeItBaby(p.root.context)
                    p.checkBox?.isChecked = !(p.checkBox?.isChecked ?: true)
                }
                val item = items[position]
                p.title?.text = item.title
                p.checkBox?.isChecked = selectedItems.contains(item)
                p.checkBox?.tag = checkNotNull(item.id).take(1)

                if (s != null) {
                    p.checkBox?.onCheckedChange { _, isChecked ->
                        when (isChecked) {
                            true -> {
                                if (p.checkBox?.tag != null && p.checkBox?.tag == "p")
                                    resetPrices(p)

                                s.itemSelected(item)
                            }
                            false -> {
                                s.itemUnselected(item)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.actionBarTitle = resources.getString(R.string.filter)
        selectedFilters.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    private fun getFilters(): FiltersResponse? {
        try {
            val api = EBashApi.create()
            val token = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            val response = api.getFilter(token).execute().body() ?: throw Exception("Invalid response")
            when {
                response.has("error") -> printError(response)
                response.has("success") -> {
                    return Gson().fromJson(response.getAsJsonObject("success"), FiltersResponse::class.java)
                }
                else -> throw Exception("Unknown response type")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        doAsync {
            val response = getFilters()
            uiThread { fragment ->
                root_layout.verticalLayout {
                    leftPadding = dip(36)
                    rightPadding = dip(36)

                    response?.filters?.forEach { filter ->
                        try {
                            val category = LayoutInflater.from(fragment.view?.context)
                                .inflate(R.layout.filter_group, this, false)

                            category?.name?.text = filter.title
                            category?.expand_button?.onClick {
                                category.expandable_layout?.toggle()
                                val from = if (category.expandable_layout?.isExpanded == true) 0f else 45f
                                val to = if (category.expandable_layout?.isExpanded == true) 45f else 0f
                                val an = RotateAnimation(
                                    from, to,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f
                                )
                                an.duration = 500
                                an.fillAfter = true
                                an.fillBefore = true
                                category.ic_group_expand_icon?.startAnimation(an)
                            }

                            if (filter.params != null) {

                                var categorySelectedItems = 0
                                category?.params_block?.layoutManager =
                                    object : GridLayoutManager(category?.params_block?.context, 2) {
                                        override fun canScrollVertically(): Boolean {
                                            return false
                                        }
                                    }
                                category?.params_block?.adapter = FilterOptionsAdapter(filter.params, selectedFilters,
                                    object : FilterOptionsAdapter.IFilterOptions {
                                        override fun itemSelected(item: FilterParam) {
                                            if (!selectedFilters.contains(item)) {
                                                selectedFilters.add(item)
                                            }
                                            categorySelectedItems += 1
                                            category?.name?.text = "${filter.title} ($categorySelectedItems)"
                                            println(selectedFilters)
                                        }

                                        override fun itemUnselected(item: FilterParam) {

                                            if (selectedFilters.contains(item)) {
                                                selectedFilters.remove(item)
                                            }
                                            categorySelectedItems -= 1
                                            if (categorySelectedItems <= 0) {
                                                category?.name?.text = "${filter.title}"
                                            } else {
                                                category?.name?.text = "${filter.title} ($categorySelectedItems)"
                                            }
                                            println(selectedFilters)
                                        }

                                        override fun setSelectedCount(count: Int) {
                                            categorySelectedItems = count
                                            if (categorySelectedItems <= 0) {
                                                category?.name?.text = "${filter.title}"
                                            } else {
                                                category?.name?.text = "${filter.title} ($categorySelectedItems)"
                                            }
                                        }
                                    })
                            }

                            /*filter.params?.forEach { param ->

                                category?.params_block?.checkBox {
                                    text = param.title
                                    /*onCheckedChange { _, isChecked ->

                                    }*/
                                }
                            }*/

                            view {
                                backgroundColor = Color.parseColor("#EAEAEA")
                            }.lparams(width = matchParent, height = dip(1))

                            this.addView(category)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                filter_show?.onClick {
                    if (selectedFilters.size > 0) {
                        val args = Bundle()
                        args.putString("filters", Gson().toJson(selectedFilters))
                        findNavController().navigate(R.id.action_filterFragment_to_filterResultFragment, args)
                    } else {
                        toast("Не выбран ни один фильтр.").show()
                    }
                }

                progress_indicator?.visibility = View.GONE
                linearLayout?.visibility = View.VISIBLE
            }
        }
    }
}