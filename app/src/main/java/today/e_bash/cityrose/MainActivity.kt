package today.e_bash.cityrose

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.content.res.ColorStateListInflaterCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.menu_basket_layout.view.*
import kotlinx.android.synthetic.main.navigation_view_header.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import today.e_bash.cityrose.events.ApiError
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.events.UpdateCart
import today.e_bash.cityrose.model.Bonus
import today.e_bash.cityrose.model.Cart
import today.e_bash.cityrose.model.Info
import today.e_bash.cityrose.tools.EBashApi
import today.e_bash.cityrose.tools.ErrorHandler


class MainActivity : AppCompatActivity() {

    private val api = EBashApi.create()
    private val accessToken = Paper.book().read<String>("accessToken") ?: ""
    private val compositeDisposable = CompositeDisposable()

    private fun setCartItemsCount(count: Int) {
        try {
            val navBottom = findViewById<BottomNavigationView?>(R.id.nav_bottom)
            val bottomCartItem = navBottom?.menu?.findItem(R.id.cartFragment)
            if (count > 0) {
                bottomCartItem?.setIcon(R.drawable.ic_not_empty_cart)
            } else {
                bottomCartItem?.setIcon(R.drawable.ic_shopping_cart)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setCartTotalPrice(price: String, count: Int) {
        try {
            val navView = findViewById<NavigationView?>(R.id.nav_view)
            val basket = navView?.menu?.findItem(R.id.cartFragment)?.actionView as? LinearLayout
            basket?.let { t ->
                val priceFormat = resources.getString(R.string.price)
                t.count?.visibility = if (count > 0) View.VISIBLE else View.GONE
                t.count?.text = String.format(priceFormat, price)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun closeNavigationView() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(Gravity.START, true)
    }

    private fun openNavigationView() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.openDrawer(Gravity.START, true)
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
                                val pointsFormat = resources.getString(R.string.points_format)
                                balance?.text = String.format(pointsFormat, bonus.success.bonus.toString())
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

    private fun loadPersonalInfo() {
        try {
            val accessToken = Paper.book()
                .read<String>("accessToken") ?: throw Exception("Don't have access token")
            val navView = findViewById<NavigationView?>(R.id.nav_view)
            val navViewHeader = navView?.getHeaderView(0)
            val name = navViewHeader?.findViewById<TextView?>(R.id.name)

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
                                name?.text = info.success.name.toString()
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

    private fun loadCartInfo() {
        try {
            api.cart(accessToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Cart.CartResponse> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(t: Cart.CartResponse) {
                        if (t.success == null) return
                        EventBus.getDefault().post(UpdateCart(t.success.goods.size, t.success.totalPrice ?: ""))
                    }

                    override fun onError(e: Throwable) {
                        EventBus.getDefault().post(ErrorEvent(e))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val navView = findViewById<NavigationView?>(R.id.nav_view)
        val navBottom = findViewById<BottomNavigationView?>(R.id.nav_bottom)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)

        navView?.setupWithNavController(navController)
        navBottom?.setupWithNavController(navController)

        navBottom?.itemIconTintList = null

        /* устанавливаем listener на иконку профиля в actionbar */
        /*val profileIcon = toolbar?.findViewById<ImageView?>(R.id.profile_icon)
        profileIcon?.setOnClickListener {
            navController.currentDestination?.id?.let { currentFragmentId ->
                if (currentFragmentId != R.id.personalAreaFragment) {
                    navController.navigate(R.id.personalAreaFragment)
                }
            }
        }*/

        /* Set action bar title font */
        val actionBarTitleView = toolbar?.findViewById<TextView?>(R.id.title)
        actionBarTitleView?.typeface = ResourcesCompat.getFont(this, R.font.montserrat_medium)

        /* устанавливаем listener на иконку меню в actionbar */
        val menuButton = toolbar?.findViewById<ImageView?>(R.id.menu_icon)
        menuButton?.let { mb ->
            mb.setOnClickListener {
                openNavigationView()
            }
        }

        navView?.setNavigationItemSelectedListener { it ->
            closeNavigationView()
            val status = NavigationUI.onNavDestinationSelected(it, navController)

            /* Если успешно перешли на новый Fragment - скрываем боковую панель */
            if (status) {
                closeNavigationView()
            }

            /* Если такой же menuItem есть и на нижнем меню - ставим его как активный */
            val item = navBottom?.menu?.findItem(it.itemId)
            navBottom?.selectedItemId = item?.itemId ?: 0

            return@setNavigationItemSelectedListener status
        }

        navBottom?.setOnNavigationItemSelectedListener {
            val status = NavigationUI.onNavDestinationSelected(it, navController)

            /* Если такой же menuItem есть и в боковом меню - ставим его как активный */
            val item = navView?.menu?.findItem(it.itemId)
            navView?.setCheckedItem(item?.itemId ?: 0)

            return@setOnNavigationItemSelectedListener status
        }

        loadBonuses()
        loadPersonalInfo()
        loadCartInfo()
    }

    override fun onSupportNavigateUp() = findNavController(R.id.nav_host_fragment).navigateUp()
    override fun onBackPressed() {
        if (!findNavController(R.id.nav_host_fragment).popBackStack()) {
            super.onBackPressed()
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
        ErrorHandler.connectionError(this, errorEvent.e)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(errorEvent: ApiError) {
        ErrorHandler.apiError(this, errorEvent.error)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(cartInfo: UpdateCart) {
        setCartTotalPrice(cartInfo.totalPrice, cartInfo.itemsCount)
        setCartItemsCount(cartInfo.itemsCount)
    }
}
