package today.e_bash.cityrose.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_about_program.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.BuildConfig
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.About
import today.e_bash.cityrose.tools.EBashApi

class AboutProgramFragment : EFragment() {

    private val compositeDisposable = CompositeDisposable()
    private val api = EBashApi.create()

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun loadData() {
        try {
            val accessToken = Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token")
            api.about(accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<About> {
                    override fun onComplete() {

                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(about: About) {
                        when {
                            about.success != null -> {
                                progress_indicator.visibility = View.INVISIBLE
                                main_content?.text = about.success
                            }
                            about.error != null -> {
                                toast(about.error.message.toString()).show()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.about_program)
        return inflater.inflate(R.layout.fragment_about_program, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        try {
            version?.text = String.format(
                "Версия %s (%s)",
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE.toUpperCase()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dev_site_button.onClick {
            val url = "http://e-bash.today/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(Intent.createChooser(i, "Открыть сайт"))
        }

        support_button.onClick {
            val emailIntent = Intent(
                Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "ezcoder@yandex.ru", null
                )
            )
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "---")
            //emailIntent.putExtra(Intent.EXTRA_TEXT, "Body")
            startActivity(Intent.createChooser(emailIntent, "Отправить письмо"))
        }
    }
}