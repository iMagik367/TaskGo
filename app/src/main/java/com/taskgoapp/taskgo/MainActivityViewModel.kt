package com.taskgoapp.taskgo

import androidx.lifecycle.ViewModel
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val authRepository: FirebaseAuthRepository
) : ViewModel()

