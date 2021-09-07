package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private val TAG = AuthenticationActivity::class.java.simpleName
    }

    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    //  you will get result here in result.data
                    Log.i(
                        TAG,
                        "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                    )
                } else Log.i(TAG, "Sign in unsuccessful")
            }

        viewModel.authenticationState.observe(this, { authenticationState ->
            authenticationState?.let {
                when (it) {
                    LoginViewModel.AuthenticationState.UNAUTHENTICATED -> {
                        Log.i(TAG, "User is not Authenticated!")
                        binding.loginButton.setOnClickListener { launchSignInFlow() }

                    }
                    LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                        Log.i(TAG, "User is Authenticated")
                        startRemindersActivity()
                    }
                }
            }
        })

        //TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

    private fun launchSignInFlow() {
        startForResult.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                    )
                ).setAuthMethodPickerLayout(
                    AuthMethodPickerLayout
                        .Builder(R.layout.layout_auth_ui)
                        .setGoogleButtonId(R.id.google_sign_in_button)
                        .setEmailButtonId(R.id.email_sign_in_button)
                        .build()
                )
                .setTheme(R.style.AppTheme)
                .build()
        )
    }

    private fun startRemindersActivity() =
        startActivity(Intent(this, RemindersActivity::class.java))
}
