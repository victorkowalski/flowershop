package today.e_bash.cityrose.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_my_bonuses.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Bonus
import today.e_bash.cityrose.tools.EBashApi

class MyBonusFragment : EFragment() {

    private val api = EBashApi.create()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.my_bonuses)
        return inflater.inflate(R.layout.fragment_my_bonuses, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun loadBonuses() {
        try {
            val accessToken = Paper.book()
                .read<String>("accessToken") ?: throw Exception("Don't have access token")
            api.bonuses(accessToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Observer<Bonus> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(bonus: Bonus) {
                        when {
                            bonus.success != null -> {
                                //val points_format = resources.getString(R.string.points)
                                progress_indicator?.visibility = View.GONE
                                tv_bonuses?.visibility = View.VISIBLE
                                tv_bonuses?.text = bonus.success.bonus.toString()
                            }
                            bonus.error != null -> {
                                toast(bonus.error.message.toString()).show()
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

        get_more_bonuses.onClick {
            findNavController().navigate(R.id.action_myBonusFragment_to_getBonusFragment)
        }

        spend_bonuses.onClick {
            findNavController().navigate(R.id.action_myBonusFragment_to_spendBonusFragment)
        }

        loadBonuses()
    }
}