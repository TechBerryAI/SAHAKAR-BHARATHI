package com.example.sahakarbharati.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.sahakarbharati.SahakarApp
import com.example.sahakarbharati.features.home.HomeViewModel
import com.example.sahakarbharati.features.profile.ProfileViewModel
import com.example.sahakarbharati.features.registration.RegistrationViewModel
import com.example.sahakarbharati.features.registration.SignInViewModel

class SahakarViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RegistrationViewModel::class.java) ->
                RegistrationViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(SignInViewModel::class.java) ->
                SignInViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(container.profileRepository, container.authRepository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.profileRepository, container.session) as T
            else -> error("Unknown ViewModel ${modelClass.name}")
        }
    }
}

@Composable
fun rememberViewModelFactory(): SahakarViewModelFactory {
    val context = LocalContext.current
    val container = (context.applicationContext as SahakarApp).container
    return SahakarViewModelFactory(container)
}

@Composable
inline fun <reified VM : ViewModel> sahakarViewModel(): VM {
    return viewModel(factory = rememberViewModelFactory())
}

fun Context.appContainer(): AppContainer = (applicationContext as SahakarApp).container
