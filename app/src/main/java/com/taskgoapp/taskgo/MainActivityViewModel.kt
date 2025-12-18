package com.taskgoapp.taskgo

import androidx.lifecycle.ViewModel
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val authRepository: FirebaseAuthRepository,
    val preferencesManager: PreferencesManager
) : ViewModel()

