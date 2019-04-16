package today.e_bash.cityrose.fragments

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_shops_on_map.*
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R

class ShopsOnMapFragment : EFragment() {

    private val mapsFragment = SOM_GmapsFragment()
    private val listFragment = SOM_ListFragment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.shops_on_map)
        return inflater.inflate(R.layout.fragment_shops_on_map, container, false)
    }

    private fun setFragment(fragment: Fragment) {
        fragmentManager?.beginTransaction()?.replace(R.id.content, fragment)?.addToBackStack(null)?.commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            rb_group?.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rb_map -> {
                        setFragment(mapsFragment)
                        (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToBottom = -1
                        (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToTop = R.id.constraintLayout
                        content?.requestLayout()
                    }
                    R.id.rb_list -> {
                        setFragment(listFragment)
                        (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToBottom = R.id.rb_group
                        (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToTop = -1
                        content?.requestLayout()
                    }
                }
            }

            // initial state
            setFragment(mapsFragment)
            (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToBottom = -1
            (content?.layoutParams as? ConstraintLayout.LayoutParams)?.topToTop = R.id.constraintLayout
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}