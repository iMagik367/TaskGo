package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.HomeBanner
import kotlinx.coroutines.flow.Flow

interface HomeBannersRepository {
    fun observeHomeBanners(): Flow<List<HomeBanner>>
}

