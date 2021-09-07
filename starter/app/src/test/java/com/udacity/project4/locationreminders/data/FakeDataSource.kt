package com.udacity.project4.locationreminders.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {
    private var shouldReturnError = false
    private val context = ApplicationProvider.getApplicationContext<Context>()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
            return Result.Error(context.getString(R.string.test_exception_message))
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error(
            context.getString(R.string.empty_list_exception_message)
        )
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
            return Result.Error(context.getString(R.string.test_exception_message))
        val found = reminders?.first { it.id == id }
        return if (found != null)
            Result.Success(found)
        else
            Result.Error("Reminder $id not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
}