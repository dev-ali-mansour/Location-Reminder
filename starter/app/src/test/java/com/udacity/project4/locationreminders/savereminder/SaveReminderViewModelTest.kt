package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import com.udacity.project4.locationreminders.util.getOrAwaitValue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SaveReminderViewModelTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var datasource: FakeDataSource

    @Before
    fun setUp() {
        stopKoin()
        datasource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), datasource)

    }

    @Test
    fun validateEnteredData_whenTitleIsEmpty_showSnackBarWithErrorMessage() =
        mainCoroutineRule.runBlockingTest {
            val reminderDataItem = ReminderDataItem(
                "",
                "Description",
                "Location",
                30.65650,
                -50.65650
            )
            val isDataValid = saveReminderViewModel.validateEnteredData(reminderDataItem)
            assertThat(isDataValid, `is`(false))
            assertThat(
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
                `is`(R.string.err_enter_title)
            )
        }

    @Test
    fun saveReminder_showLoadingWhileSavingAndReDisableItAfterFinishSaving() =
        mainCoroutineRule.runBlockingTest {
            mainCoroutineRule.pauseDispatcher()
            val reminderDataItem = ReminderDataItem(
                "Reminder",
                "Description",
                "Location",
                50.32350,
                -30.66467
            )
            saveReminderViewModel.saveReminder(
                reminderDataItem
            )
            assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
            mainCoroutineRule.resumeDispatcher()
            assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        }
}