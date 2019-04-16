package today.e_bash.cityrose

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_error.*
import org.jetbrains.anko.getStackTraceString
import today.e_bash.cityrose.tools.ErrorHandler

class ErrorActivity : AppCompatActivity() {

    var error: Throwable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)
        error = ErrorHandler.e
        try {
            errorView?.text = error?.localizedMessage
            Log.e("ErrorActivity", ErrorHandler.e?.getStackTraceString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}