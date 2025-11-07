package com.taskgoapp.taskgo.data.local.converter

import androidx.room.TypeConverter
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.OrderStatus

class Converters {
    
    @TypeConverter
    fun fromAccountType(accountType: AccountType): String {
        return accountType.name
    }

    @TypeConverter
    fun toAccountType(accountType: String): AccountType {
        return AccountType.valueOf(accountType)
    }

    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String {
        return status.name
    }

    @TypeConverter
    fun toOrderStatus(status: String): OrderStatus {
        return OrderStatus.valueOf(status)
    }
}
