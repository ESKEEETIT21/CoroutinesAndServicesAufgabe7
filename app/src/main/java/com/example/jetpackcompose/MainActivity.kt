package com.example.jetpackcompose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpackcompose.viewmodel.WeatherViewModel
import com.example.jetpackcompose.ui.WeatherApp
import com.example.jetpackcompose.viewmodel.PopupServiceManager

class MainActivity : ComponentActivity() {

    private val popupServiceManager = PopupServiceManager(this)

    /**
     * Initializes the activity and sets up the content view.
     *
     * @param savedInstanceState Restores previous state if available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Todo Uncomment this line
        handlePopupService()

        setContent {
            val viewModel: WeatherViewModel = viewModel()
            WeatherApp(viewModel)
        }
    }

    /**
     * Manages permissions and starts the popup service based on the Android version.
     */
    private fun handlePopupService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            popupServiceManager.requestPermission()
        } else {
            popupServiceManager.startPopupService()
        }
    }
}