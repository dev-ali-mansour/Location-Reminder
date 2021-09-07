package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDB() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDB() = database.close()

    @Test
    fun saveReminder_whenGetItFromById_thenMatches() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "Reminder",
            description = "Description",
            location = "Location",
            latitude = 23.4532,
            longitude = 4854.4532
        )
        database.reminderDao().saveReminder(reminder)
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveReminder_whenDeleteAllReminders_thenDatabaseReminderListIsEmpty() =
        runBlockingTest {
            val reminder = ReminderDTO(
                title = "Reminder",
                description = "Description",
                location = "Location",
                latitude = 23.4532,
                longitude = 4854.4532
            )

            database.reminderDao().saveReminder(reminder)
            database.reminderDao().deleteAllReminders()
            val reminders = database.reminderDao().getReminders()
            assertThat(reminders.isEmpty(), `is`(true))
        }

    @Test
    fun getReminderById_whenPassingNotFoundId_thenReturnNullValue() = runBlockingTest {
        val reminder = database.reminderDao().getReminderById("5")

        assertThat(reminder, nullValue())
    }
}