package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest :
    AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        startKoin {
            modules(listOf(myModule))
        }
        repository = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResources() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    @Test
    fun addReminderEndToEndTest() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Try to save reminder without enter title
        onView(withId(R.id.saveReminder)).perform(click())
        //Show SnackBar alert "Please enter title"
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
        delay(2000)
        // Enter reminder title and save reminder
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(appContext.getString(R.string.reminder_test_title)))
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(appContext.getString(R.string.reminder_test_descreiption)))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.save_location_button)).perform(click())

        // Validate EditText values
        onView(withText(appContext.getString(R.string.reminder_test_title)))
            .check(matches(isDisplayed()))
        onView(withText(appContext.getString(R.string.reminder_test_descreiption)))
            .check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).perform(click())

        // Test Show Toast informing user "Reminder Saved !"
        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not((getActivity(activityScenario)?.window?.decorView)))).check(matches(isDisplayed()))
        activityScenario.close()
    }
}