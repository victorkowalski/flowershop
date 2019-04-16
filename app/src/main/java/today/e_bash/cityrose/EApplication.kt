package today.e_bash.cityrose

import android.app.Application
import android.util.Log
import io.paperdb.Paper
import ss.com.bannerslider.Slider
import today.e_bash.cityrose.tools.GlideImageLoadingService

@Suppress("unused")
class EApplication : Application() {
    companion object {
        const val TAG = "EApplication"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            /* Images Slider */
            Slider.init(GlideImageLoadingService(applicationContext))

            /* Storage */
            Paper.init(applicationContext)

        } catch (e: Exception) {
            Log.e(TAG, e.localizedMessage)
        }
    }
}