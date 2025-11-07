package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.UserProfileEntity
import com.taskgoapp.taskgo.core.model.UserProfile
import com.taskgoapp.taskgo.core.model.AccountType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UserMapper {
    
    fun UserProfileEntity.toModel(): UserProfile {
        val profileImages = try {
            profileImages?.let { 
                Gson().fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type) 
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        return UserProfile(
            id = this.id,
            name = this.name,
            email = this.email,
            phone = this.phone,
            city = this.city,
            profession = this.profession,
            accountType = when (this.accountType) {
                "PRESTADOR" -> AccountType.PRESTADOR
                "VENDEDOR" -> AccountType.VENDEDOR
                "CLIENTE" -> AccountType.CLIENTE
                else -> AccountType.CLIENTE
            },
            rating = this.rating,
            avatarUri = this.avatarUri,
            profileImages = profileImages
        )
    }
    
    fun UserProfile.toEntity(): UserProfileEntity {
        val profileImagesJson = profileImages?.let { 
            Gson().toJson(it) 
        }
        
        return UserProfileEntity(
            id = this.id,
            name = this.name,
            email = this.email,
            phone = this.phone,
            city = this.city,
            profession = this.profession,
            accountType = this.accountType.name,
            rating = this.rating,
            avatarUri = this.avatarUri,
            profileImages = profileImagesJson
        )
    }
}