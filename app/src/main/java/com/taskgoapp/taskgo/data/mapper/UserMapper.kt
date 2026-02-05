package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.UserProfileEntity
import com.taskgoapp.taskgo.core.model.UserProfile
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
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
            state = this.state,
            profession = this.profession,
            accountType = when (this.accountType) {
                "PARCEIRO" -> AccountType.PARCEIRO
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
            state = this.state,
            profession = this.profession,
            accountType = this.accountType.name,
            rating = this.rating,
            avatarUri = this.avatarUri,
            profileImages = profileImagesJson
        )
    }
    
    fun UserFirestore.toModel(): UserProfile {
        // Mapear role string para AccountType enum
        // CRÍTICO: O role SEMPRE será "partner" ou "client" - garantido pelo sistema
        val accountType = if (this.role.lowercase() == "partner") {
            AccountType.PARCEIRO
        } else {
            AccountType.CLIENTE
        }
        
        // Lei 1: city e state DEVEM estar na raiz do documento users/{userId}
        // NÃO usar address.city ou address.state - isso viola a Lei 1
        // ✅ REMOVIDO: LocationUpdateService não atualiza mais city/state via GPS
        // City/state vêm APENAS do cadastro do usuário no Firestore
        val city = this.city?.takeIf { it.isNotBlank() }
        val state = this.state?.takeIf { it.isNotBlank() }
        
        return UserProfile(
            id = this.uid,
            name = this.displayName ?: "",
            email = this.email,
            phone = this.phone,
            city = city,
            state = state,
            profession = null, // UserFirestore não tem profession diretamente
            accountType = accountType,
            rating = this.rating,
            avatarUri = this.photoURL,
            profileImages = emptyList()
        )
    }
}