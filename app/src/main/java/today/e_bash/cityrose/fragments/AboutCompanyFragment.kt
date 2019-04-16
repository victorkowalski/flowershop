package today.e_bash.cityrose.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_about_company.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Company
import today.e_bash.cityrose.tools.EBashApi

class AboutCompanyFragment : EFragment() {

    private val compositeDisposable = CompositeDisposable()
    private val api = EBashApi.create()

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.about_company)
        return inflater.inflate(R.layout.fragment_about_company, container, false)
    }

    private fun loadData() {
        try {
            val token = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            api.company(token)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Observer<Company> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(company: Company) {
                        when {
                            company.success != null -> {
                                progress_indicator?.visibility = View.GONE
                                main_content?.text = company.success
                            }
                            company.error != null -> {
                                toast(company.error.message.toString()).show()
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
        loadData()
    }

}