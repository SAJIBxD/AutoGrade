package com.autograde.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autograde.AutoGradeApp
import com.autograde.data.AuthRepository
import com.autograde.data.FileRepository
import com.autograde.data.ProfileRepository
import com.autograde.ui.assessment.AssessmentViewModel

class AppViewModelFactory(private val app: AutoGradeApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepo = AuthRepository(app.api, app.tokenStore)
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepo) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(ProfileRepository(app.api), authRepo, app.tokenStore) as T
            modelClass.isAssignableFrom(AssessmentViewModel::class.java) ->
                AssessmentViewModel(FileRepository(app.api)) as T
            else -> throw IllegalArgumentException("Unknown VM: $modelClass")
        }
    }
}
