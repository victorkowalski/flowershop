package today.e_bash.cityrose.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.paperdb.Paper
import today.e_bash.cityrose.AuthActivity
import today.e_bash.cityrose.ErrorActivity
import today.e_bash.cityrose.model.Error

class ErrorHandler {

    enum class ApiErrorCodes(val id: Int) {
        EXPIRED_TOKEN(4012)
    }

    companion object {
        var e: Throwable? = null
        fun connectionError(ctx: Context, e: Throwable) {
            try {
                this.e = e
                val intent = Intent(ctx, ErrorActivity::class.java)
                ctx.startActivity(intent)
                (ctx as? Activity)?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun apiError(ctx: Context, e: Error) {
            try {
                when (e.error) {
                    ApiErrorCodes.EXPIRED_TOKEN.id -> {
                        val intent = Intent(ctx, AuthActivity::class.java)
                        Paper.book().delete("accessToken")
                        ctx.startActivity(intent)
                        (ctx as? Activity)?.finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}