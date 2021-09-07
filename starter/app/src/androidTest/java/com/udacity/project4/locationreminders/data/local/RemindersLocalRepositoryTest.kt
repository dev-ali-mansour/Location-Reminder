package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.MainAndroidTestCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var mainCoroutineRule = MainAndroidTestCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase
    private lateinit var remindersDAO: RemindersDao
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        remindersDAO = database.reminderDao()
        repository =
            RemindersLocalRepository(
                remindersDAO,
                Dispatchers.Main
            )
    }

    @After
    fun closeDB() = database.close()

    @Test
    fun saveReminder_whenGetItByID_thenMatches() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDTO(
            title = "Reminder",
            description = "Description",
            location = "Location",
            latitude = 23.4532,
            longitude = 4854.4532
        )
        repository.saveReminder(reminder)
        val reminderLoaded = repository.getReminder(reminder.id) as Result.Success<ReminderDTO>
        val loaded = reminderLoaded.data

        assertThat(loaded, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveReminder_whenDeleteAllReminders_thenRemindersListIsEmpty() =
        mainCoroutineRule.runBlockingTest {
            val reminder = ReminderDTO(
                title = "Reminder",
                description = "Description",
                location = "Location",
                latitude = 23.4532,
                longitude = 4854.4532
            )
            repository.saveReminder(reminder)
            repository.deleteAllReminders()
            val reminders = repository.getReminders() as Result.Success<List<ReminderDTO>>
            val data = reminders.data
            assertThat(data.isEmpty(), `is`(true))

        }

    @Test
    fun getReminder_whenReminderIdNotFound_thenMessageIsReminderNotFound() =
        mainCoroutineRule.runBlockingTest {
            val reminder = repository.getReminder("5") as Result.Error
            assertThat(reminder.message, notNullValue())
            assertThat(reminder.message, `is`("Reminder not found!"))
        }
}