package today.e_bash.cityrose.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.jetbrains.anko.doAsync
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import kotlinx.android.synthetic.main.fragment_get_bonuses.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class GetBonusFragment: EFragment() {

    val TAG = "BonusFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.my_bonuses)
        return inflater.inflate(R.layout.fragment_get_bonuses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}