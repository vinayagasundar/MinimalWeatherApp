/*
 * Copyright 2017 vinayagasundar
 * Copyright 2017 randhirgupta
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package you.devknights.minimalweather.ui.landing


import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import java.util.Calendar

import javax.inject.Inject

import you.devknights.minimalweather.R
import you.devknights.minimalweather.database.entity.WeatherEntity
import you.devknights.minimalweather.di.Injectable
import you.devknights.minimalweather.model.Resource
import you.devknights.minimalweather.model.Status
import you.devknights.minimalweather.util.UnitConvUtil


/**
 * A simple [Fragment] subclass.
 */
class LandingFragment : Fragment(), Injectable {

    private var mCityText: TextView? = null
    private var mTimeText: TextView? = null

    private var mWeatherStatusImage: ImageView? = null
    private var mWeatherTemperatureText: TextView? = null
    private var mTimeReleatedText: TextView? = null

    private var mSunriseText: TextView? = null
    private var mWindText: TextView? = null
    private var mTemperatureText: TextView? = null

    private var mLoadingProgressBar: ProgressBar? = null
    private var mDetailContainer: View? = null

    @Inject
    lateinit var mFactory: ViewModelProvider.Factory

    @Inject
    lateinit var mLandingViewModel: LandingViewModel



    private val isPermissionGranted: Boolean
        get() = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater?.inflate(R.layout.fragment_landing, container, false)
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCityText = view?.findViewById(R.id.cityText)
        mTimeText = view?.findViewById(R.id.timeText)
        mWeatherStatusImage = view?.findViewById(R.id.weatherStatusImage)
        mWeatherTemperatureText = view?.findViewById(R.id.weatherTemperatureText)

        mTimeReleatedText = view?.findViewById(R.id.timeRelatedMessageText)
        mSunriseText = view?.findViewById(R.id.sunriseText)
        mWindText = view?.findViewById(R.id.windText)
        mTemperatureText = view?.findViewById(R.id.temperatureText)

        mLoadingProgressBar = view?.findViewById(R.id.loadingProgressBar)
        mDetailContainer = view?.findViewById(R.id.detailContainer)

        mLoadingProgressBar?.visibility = View.VISIBLE

        mLandingViewModel = ViewModelProviders.of(this, mFactory)
                .get(LandingViewModel::class.java)

        if (!isPermissionGranted) {
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        mLandingViewModel.location.observe(this, Observer<Location> {
            it?.let {
                this.getDataFromLocation(it)
            }
        })
    }

    private fun getDataFromLocation(location: Location) {
        val resourceLiveData = mLandingViewModel
                .getWeatherData(location)


        resourceLiveData.observe(this, Observer<Resource<WeatherEntity>> { weatherEntityResource ->
            if (weatherEntityResource != null && weatherEntityResource.status == Status.SUCCESS) {
                weatherEntityResource.data?.let {
                    bindData(it)
                }
            }
        })
    }


    private fun bindData(weather: WeatherEntity) {
        mCityText?.text = weather.placeName
        mTimeText?.text = DateFormat.format("EEEE, hh:mm a", Calendar.getInstance()
                .timeInMillis)

        val temperatureInCelsius = getString(R.string.temp_in_celsius,
                UnitConvUtil.kelvinToCelsius(weather.temperature))

        mWeatherTemperatureText?.text = temperatureInCelsius

        val timeInMills = weather.sunriseTime * 1000

        mSunriseText?.text = DateFormat.format("hh.mm", timeInMills)
        mWindText?.text = getString(R.string.wind_speed_in_miles, weather.windSpeed)
        mTemperatureText?.text = temperatureInCelsius


        mWeatherStatusImage?.setImageResource(getWeatherIcon(weather.weatherIcon))

        mDetailContainer?.alpha = 0f
        mDetailContainer?.visibility = View.VISIBLE
        mLoadingProgressBar?.visibility = View.GONE

        mDetailContainer?.animate()?.alpha(1f)?.setDuration(400)?.start()
    }


    @DrawableRes
    private fun getWeatherIcon(icon: String?): Int {
        when (icon) {
            "01d", "01n" -> return R.drawable.clear_sky

            "02d", "02n" -> return R.drawable.few_clouds

            "03d", "03n" -> return R.drawable.scattered_clouds

            "04d", "04n" -> return R.drawable.broken_clouds

            "09d", "09n" -> return R.drawable.shower_rain


            "10d", "10n" -> return R.drawable.rain


            "11d", "11n" -> return R.drawable.thunderstorm


            "13d", "13n" -> return R.drawable.snow


            "50d", "50n" -> return R.drawable.mist

            else -> return R.drawable.clear_sky
        }
    }

    companion object {

        private val PERMISSION_REQUEST_CODE = 1420
        private val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

        private val TAG = "LandingFragment"
    }
}// Required empty public constructor
