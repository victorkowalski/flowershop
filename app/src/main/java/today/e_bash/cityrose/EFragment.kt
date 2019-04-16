package today.e_bash.cityrose

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import today.e_bash.cityrose.fragments.CatalogFragment
import today.e_bash.cityrose.fragments.FilterResultFragment

open class EFragment : Fragment() {
    private val TAG = "EFragment"

    var actionBarTitle = ""

    fun printError(payload: JsonObject) {
        doAsync {
            uiThread {
                try {
                    val errorObject = payload.getAsJsonObject("error")
                    if (errorObject.has("message")) {
                        toast(
                            String.format(
                                getString(R.string.error),
                                errorObject.get("message").toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setFilterIconVisible(v: Int) {
        val toolbar = activity?.toolbar
        val filterIcon = toolbar?.findViewById<ImageView>(R.id.filter_icon)
        filterIcon?.visibility = v
    }

    private fun setFilterIconClickListener(listener: () -> Unit) {
        try {
            val filterIcon = activity?.toolbar?.findViewById<ImageView>(R.id.filter_icon)
            filterIcon?.onClick {
                /*val args = Bundle()
                navController.navigate(R.id.filterFragment, args)*/
                listener()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        when {
            this is CatalogFragment -> {
                setFilterIconVisible(View.VISIBLE)
                this.setFilterIconClickListener {
                    findNavController().navigate(R.id.filterFragment)
                }
            }
            this is FilterResultFragment -> {
                setFilterIconVisible(View.VISIBLE)
                this.setFilterIconClickListener {
                    activity?.onBackPressed()
                }
            }
            else -> setFilterIconVisible(View.GONE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = activity?.toolbar
        val textView = toolbar?.findViewById<TextView>(R.id.title)
        textView?.text = this.actionBarTitle
    }
}