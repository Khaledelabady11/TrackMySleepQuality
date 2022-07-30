/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNight
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {
                private var viewModelJob=Job()
        override fun onCleared() {
                super.onCleared()
                viewModelJob.cancel()
        }
        private val uiScope= CoroutineScope(Dispatchers.Main+viewModelJob)
        private var toNight = MutableLiveData<SleepNight>()
        private val nights=database.getAllNights()
        val nightsString = Transformations.map(nights) { nights ->
                formatNight(nights, application.resources)
        }
        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

        val navigateToSleepQuality: LiveData<SleepNight>
                get() = _navigateToSleepQuality

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }
        val startButtonVisible = Transformations.map(toNight) {
                null == it
        }
        val stopButtonVisible = Transformations.map(toNight) {
                null != it
        }
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        init {
            initializeToNighr()
        }

        private fun initializeToNighr() {
                uiScope.launch {
                        toNight.value=getToNightFromDatabase()
                }
        }

        private suspend fun getToNightFromDatabase(): SleepNight? {
                return withContext(Dispatchers.IO) {
                        var night = database.getTonight()
                        if (night?.endTimeMilli != night?.startTimeMilli) {
                                night = null
                        }
                        night
                }
        }

        fun onStartTracking(){
                uiScope.launch {
                        var newNight=SleepNight()
                        insert(newNight)
                        toNight.value=getToNightFromDatabase()

                }
        }

        private suspend fun insert(newNight: SleepNight) {
                withContext(Dispatchers.IO){
                        database.insert(newNight)
                }
        }
        fun onStopTracking(){
                uiScope.launch {
                        val oldNight=toNight.value ?: return@launch
                        oldNight.endTimeMilli=System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight

                }
        }

        private suspend fun update(oldNight: SleepNight) {
                withContext(Dispatchers.IO){
                        database.update(oldNight)
                }
        }
        fun onClear(){
                uiScope.launch {
                        toNight.value=null
                }
        }
        suspend fun clear(){
                withContext(Dispatchers.IO){
                        database.clear()
                }
        }

}

