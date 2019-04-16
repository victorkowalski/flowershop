package today.e_bash.cityrose.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import androidx.navigation.fragment.findNavController
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_personal_area.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.AuthActivity
import today.e_bash.cityrose.EFragment
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Info
import today.e_bash.cityrose.model.InfoSuccess
import today.e_bash.cityrose.tools.EBashApi
import java.text.SimpleDateFormat
import java.time.Year
import java.util.*

class PersonalAreaFragment : EFragment() {


    private val api = EBashApi.create()
    private val compositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun setUiData(profile: InfoSuccess) {
        try {
            last_name.setText(profile.lastName)
            first_name.setText(profile.name)
            sure_name.setText(profile.sureName)
            phone.setText(profile.phone)
            birthdate.setText(profile.birthday)
            agreement.isChecked = profile.agreement?.toInt() == 1

            gender_radio.clearCheck()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.actionBarTitle = resources.getString(R.string.personal_area)
        return inflater.inflate(R.layout.fragment_personal_area, container, false)
    }

    private fun loadData() {
        try {
            val accessToken = Paper.book()
                .read<String>("accessToken") ?: throw Exception("Don't have access token")
            api.info(accessToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Observer<Info> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(info: Info) {
                        when {
                            info.success != null -> {
                                setUiData(info.success)
                            }
                            info.error != null -> {
                                toast(info.error.message.toString()).show()
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

    private fun bindSaveButton() {
        try {
            val accessToken = Paper.book()
                .read<String>("accessToken") ?: throw Exception("Don't have access token")
            val first_name_s = first_name.text.toString()
            val last_name_s = last_name.text.toString()
            val sure_name_s = sure_name.text.toString()
            val phone_s = phone.text.toString()
            val birthdate_s = birthdate.text.toString()
            val agreement_s = if (agreement.isChecked) "1" else "0"

            api.update(
                first_name_s, last_name_s, sure_name_s,
                phone_s, agreement_s, "1", birthdate_s, accessToken
            ).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Info> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(info: Info) {
                        when {
                            info.success != null -> {
                                try {
                                    setUiData(info.success)

                                    val alertDialog = AlertDialog
                                        .Builder(this@PersonalAreaFragment.context)
                                        .create()

                                    val view = LayoutInflater.from(this@PersonalAreaFragment.context)
                                        .inflate(R.layout.dialog_save_personal_data, null)
                                    val continueButton = view.findViewById<Button>(R.id.continue_button)
                                    continueButton.onClick {
                                        findNavController().navigate(R.id.catalogFragment)
                                        alertDialog.dismiss()
                                    }

                                    alertDialog.setView(view)
                                    alertDialog?.show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            info.error != null -> {
                                toast(info.error.message.toString()).show()
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

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadData()
        save_button.onClick { bindSaveButton() }

        exit_button.onClick {
            try {
                Paper.book().delete("accessToken")
                val intent = Intent(activity, AuthActivity::class.java)
                startActivity(intent)
                activity?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        birthdate?.keyListener = null
        birthdate?.onClick {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            if (birthdate?.context != null) {
                val datePicker = DatePickerDialog(
                    birthdate.context, DatePickerDialog.OnDateSetListener { _, y, m, d ->
                        calendar.set(Calendar.YEAR, y)
                        calendar.set(Calendar.MONTH, m)
                        calendar.set(Calendar.DAY_OF_MONTH, d)
                        val formatter = SimpleDateFormat("dd.MM.yyyy")
                        birthdate?.setText(formatter.format(calendar.time))
                    }, year, month, day
                )
                datePicker.show()
            }
        }

        try {
            gender_radio.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.man -> {
                        inactive_bottom_indicator_man.backgroundColor = Color.parseColor("#01B1EC")
                        inactive_bottom_indicator_woman.backgroundColor = Color.parseColor("#EBEBEB")
                    }
                    R.id.woman -> {
                        inactive_bottom_indicator_woman.backgroundColor = Color.parseColor("#01B1EC")
                        inactive_bottom_indicator_man.backgroundColor = Color.parseColor("#EBEBEB")
                    }
                    else -> {
                        inactive_bottom_indicator_woman.backgroundColor = Color.parseColor("#EBEBEB")
                        inactive_bottom_indicator_man.backgroundColor = Color.parseColor("#EBEBEB")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}