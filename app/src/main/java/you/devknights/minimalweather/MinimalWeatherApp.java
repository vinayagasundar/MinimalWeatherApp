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

package you.devknights.minimalweather;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import you.devknights.minimalweather.core.executor.AppExecutors;
import you.devknights.minimalweather.database.AppDatabase;
import you.devknights.minimalweather.database.WeatherDatabase;
import you.devknights.minimalweather.database.dao.WeatherDAO;
import you.devknights.minimalweather.database.entity.WeatherEntity;
import you.devknights.minimalweather.di.AppInjector;

/**
 * {@link Application} instance of the Weather App.
 * we can initialize all the library here.
 * @author vinayagasundar
 */

public class MinimalWeatherApp extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    static {
        // Initialize the day & light mode in the App
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AppInjector.init(this);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    private void checkForExpiredData() {
        LiveData<Boolean> isDbCreated = AppDatabase.getInstance().isDatabaseCreated();
        isDbCreated.observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean != null && aBoolean) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        WeatherDatabase database = AppDatabase.getInstance().getDatabase();
                        if (database != null) {
                            WeatherDAO weatherDAO = database.weatherDAO();
                            List<WeatherEntity> weatherEntities = weatherDAO
                                    .getAllExpiredData(System.currentTimeMillis());

                            if (weatherEntities != null && weatherEntities.size() > 0) {
                                weatherDAO.deleteWeatherData(weatherEntities);
                            }
                        }
                    });
                    isDbCreated.removeObserver(this);
                }
            }
        });
    }
}
