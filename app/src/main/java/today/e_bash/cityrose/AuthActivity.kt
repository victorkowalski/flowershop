package today.e_bash.cityrose

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import br.com.sapereaude.maskedEditText.MaskedEditText
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_auth.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.sdk27.coroutines.onClick
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Login
import today.e_bash.cityrose.model.PIN
import today.e_bash.cityrose.tools.EBashApi
import today.e_bash.cityrose.tools.ErrorHandler


class AuthActivity : AppCompatActivity() {
    companion object {
        const val TAG = "AuthActivity"
    }

    private val compositeDisposable = CompositeDisposable()
    private val api = EBashApi.create()

    private val countDownTimer = object : CountDownTimer(10000, 1000) {
        override fun onFinish() {
            runOnUiThread {
                try {
                    send_again_button?.isEnabled = true
                    resend_via?.text = ""
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            runOnUiThread {
                try {
                    val min = Math.ceil((millisUntilFinished / 60000).toDouble())
                    val sec = Math.ceil(((millisUntilFinished / 1000) % 60).toDouble())

                    val format = resources.getString(R.string.resend_via)
                    resend_via?.text = String.format(format, min, sec)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun goToMainActivity() {
        try {
            val intent = Intent(this@AuthActivity, MainActivity::class.java)
            this@AuthActivity.startActivity(intent)
            this@AuthActivity.finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkToken(): Boolean {
        try {
            @Suppress("UNUSED_VARIABLE")
            val accessToken = Paper.book().read<String>("accessToken") ?: return false

            //check token is active
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun requirePin() {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(phoneTextEdit.text.toString(), "RU")
            val number = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164).toString()

            continue_button?.visibility = View.GONE
            send_again_button.isEnabled = false
            countDownTimer.cancel()
            countDownTimer.start()

            try {
                val method = if (BuildConfig.DEBUG) {
                    api.getPin("${phoneTextEdit.text} test", "1")
                } else {
                    api.getPin(number, "0")
                }

                method.observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(object : Observer<PIN> {
                        override fun onComplete() {
                        }

                        override fun onSubscribe(d: Disposable) {
                            compositeDisposable.add(d)
                        }

                        override fun onNext(pin: PIN) {
                            when {
                                pin.success != null -> {
                                    Paper.book().write("authToken", pin.success.token)
                                    send_code_form?.visibility = View.VISIBLE
                                    if (BuildConfig.DEBUG) {
                                        pinTextEdit?.setText(pin.success.pin)
                                    }
                                }
                                pin.error != null -> {
                                    pin_error_label?.text = pin.error.message
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
        } catch (e: NumberParseException) {
            Toast.makeText(this@AuthActivity, R.string.invalid_phone_number, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendPin() {
        try {
            val authToken = Paper.book().read<String>("authToken") ?: throw Exception("Don't have auth token")
            api.login(pinTextEdit.text.toString(), authToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Observer<Login> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(login: Login) {
                        when {
                            login.success != null -> {
                                Paper.book().write("accessToken", login.success.token)
                                goToMainActivity()
                            }
                            login.error != null -> {
                                pin_error_label?.text = login.error.message
                                pin_error_label?.visibility = View.VISIBLE
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: NumberParseException) {
            Toast.makeText(this@AuthActivity, R.string.invalid_phone_number, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        if (checkToken()) {
            this.goToMainActivity()
            return
        }

        if (BuildConfig.DEBUG) {
            continue_button.isEnabled = true
            phoneTextEdit.setText(R.string.test_phone_number)
        }

        register_in_sys?.context?.let { ctx ->
            register_in_sys.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
        }

        auth_screen_message?.context?.let { ctx ->
            auth_screen_message?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
        }

        auth_enter_pin_title?.context?.let { ctx ->
            auth_enter_pin_title?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_semibold)
        }

        resend_via?.context?.let { ctx ->
            resend_via?.typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_medium)
        }

        phoneTextEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val phoneUtil = PhoneNumberUtil.getInstance()
                try {
                    val numberProto = phoneUtil.parse(s.toString(), "RU")
                    continue_button.isEnabled =
                        phoneUtil.isValidNumber(numberProto) &&
                                phoneUtil.getNumberType(numberProto) == PhoneNumberUtil.PhoneNumberType.MOBILE

                    if (BuildConfig.DEBUG) {
                        continue_button.isEnabled = true
                    }
                } catch (e: NumberParseException) {
                    Log.e(TAG, "NumberParseException was thrown: $e")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        pinTextEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val reg = Regex("[0-9]")
                s?.let {
                    send_pin_button.isEnabled = reg.containsMatchIn(it.toString())
                }
            }
        })

        continue_button.onClick {
            requirePin()
        }

        send_again_button?.onClick {
            //requirePin()

            val validatePhone = AlertDialog.Builder(this@AuthActivity).create()
            val view = LayoutInflater.from(this@AuthActivity).inflate(R.layout.confirm_form_activity_auth, null)
            val dialogPhoneTextEdit = view.findViewById<MaskedEditText?>(R.id.phoneTextEdit)
            val allRightButton = view.findViewById<Button?>(R.id.allRightButton)
            dialogPhoneTextEdit?.setText(phoneTextEdit?.text?.replace(Regex("^\\+7"), ""))
            allRightButton?.onClick {
                phoneTextEdit.setText(dialogPhoneTextEdit?.text?.replace(Regex("^\\+7"), ""))
                validatePhone.dismiss()
                requirePin()
            }
            validatePhone.setView(view)
            validatePhone.show()
        }

        send_pin_button.onClick {
            sendPin()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(errorEvent: ErrorEvent) {
        EventBus.getDefault().unregister(this)
        ErrorHandler.connectionError(this, errorEvent.e)
    }
}