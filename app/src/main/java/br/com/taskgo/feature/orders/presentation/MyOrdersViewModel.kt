package br.com.taskgo.taskgo.feature.orders.presentation

import androidx.lifecycle.ViewModel
import com.example.taskgoapp.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    val orderRepository: OrderRepository
) : ViewModel()




