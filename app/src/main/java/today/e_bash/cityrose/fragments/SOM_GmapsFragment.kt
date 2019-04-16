package today.e_bash.cityrose.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.paperdb.Paper
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_shops_on_map_gmaps.*
import kotlinx.android.synthetic.main.fragment_shops_on_map_gmaps.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast
import today.e_bash.cityrose.R
import today.e_bash.cityrose.events.ErrorEvent
import today.e_bash.cityrose.model.Addresses
import today.e_bash.cityrose.model.AddressesSuccess
import today.e_bash.cityrose.tools.EBashApi

class SOM_GmapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val api = EBashApi.create()
    private val compositeDisposable = CompositeDisposable()

    data class ZoomLevels(
        val World: Float = 1F,
        val LandmassContinent: Float = 5F,
        val City: Float = 10F,
        val Streets: Float = 15F,
        val Buildings: Float = 20F
    )

    private var mapFragment: SupportMapFragment? = null
    private var googleMap: GoogleMap? = null
    private val markets: HashMap<Marker?, AddressesSuccess> = hashMapOf()

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shops_on_map_gmaps, container, false)
    }

    override fun onResume() {
        super.onResume()
        sliding_layout?.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            info_block_wrapper?.layoutParams?.height = resources.displayMetrics.heightPixels / 2
            info_block_wrapper?.requestLayout()

            mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addMark(p: AddressesSuccess) {
        try {
            val markerOptions = MarkerOptions()
                .position(
                    LatLng(
                        p.latitude?.toDoubleOrNull() ?: .0,
                        p.longitude?.toDoubleOrNull() ?: .0
                    )
                )
                .title(p.name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
            val marker = googleMap?.addMarker(markerOptions)
            markets[marker] = p
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAddresses() {
        try {
            val accessToken =
                Paper.book().read<String>("accessToken") ?: throw Exception("Don't have access token in storage")
            api.addresses(accessToken)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Addresses> {
                    override fun onComplete() {
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable.add(d)
                    }

                    override fun onNext(addresses: Addresses) {
                        when {
                            addresses.success != null -> {
                                addresses.success.forEach { point ->
                                    if (point != null) {
                                        addMark(point)
                                    }
                                }
                            }
                            addresses.error != null -> {
                                toast(addresses.error.message.toString()).show()
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

    override fun onMapReady(map: GoogleMap?) {
        try {
            googleMap = map
            map?.setOnMarkerClickListener(this)
            loadAddresses()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMarkerClick(market: Marker?): Boolean {

        try {
            val addressItem = markets[market]
            val zoomLevels = ZoomLevels()
            googleMap?.moveCamera(CameraUpdateFactory.zoomTo(zoomLevels.City))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(market?.position))

            sliding_layout?.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
            sliding_layout?.info_block?.removeAllViews()
            sliding_layout?.info_block?.verticalLayout {
                verticalLayout {
                    textView {
                        text = addressItem?.name
                        typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_medium)
                        textSize = 16f
                        textColor = Color.BLACK
                    }.lparams {
                        topMargin = dip(30)
                        bottomMargin = dip(0)
                    }

                    textView {
                        text = addressItem?.address
                        textSize = 12f
                        textColor = Color.parseColor("#787878")
                    }.lparams {
                        topMargin = dip(4)
                        bottomMargin = dip(30)
                    }

                }.lparams(width = matchParent, height = wrapContent) {
                    leftMargin = dip(35)
                    rightMargin = dip(35)
                }

                view {
                    backgroundColor = Color.parseColor("#80BDCFD5")
                }.lparams(width = matchParent, height = dip(1))

                verticalLayout {
                    textView {
                        text = "Телефон"
                        textColor = Color.parseColor("#787878")
                        textSize = 12f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(24)
                    }

                    textView {
                        text = addressItem?.phone
                        textColor = Color.BLACK
                        textSize = 14f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(3)
                    }

                    textView {
                        text = "Режим работы"
                        textColor = Color.parseColor("#787878")
                        textSize = 12f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(18)
                    }

                    textView {
                        text = addressItem?.workingHours
                        textColor = Color.BLACK
                        textSize = 14f
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(3)
                    }

                    button {
                        text = resources.getString(R.string.build_a_route)
                        textSize = 12f
                        textColor = Color.WHITE
                        typeface = ResourcesCompat.getFont(this.context, R.font.montserrat_bold)
                        background = resources.getDrawable(R.drawable.button_background, null)
                        onClick {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("google.navigation:q=${addressItem?.latitude},${addressItem?.longitude}")
                            )
                            this@button.context.startActivity(intent)
                        }
                    }.lparams(width = matchParent, height = wrapContent) {
                        topMargin = dip(25)
                    }
                }.lparams(width = matchParent, height = wrapContent) {
                    leftMargin = dip(35)
                    rightMargin = dip(35)
                }

                leftPadding = dip(0)
                rightPadding = dip(0)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}