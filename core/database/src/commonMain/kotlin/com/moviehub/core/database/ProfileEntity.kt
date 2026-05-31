package com.moviehub.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.moviehub.core.model.Profile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String?,
    val pin: String?,
    val isChild: Boolean,
    val createdAt: Long,
)

fun ProfileEntity.toExternalModel() = Profile(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    pin = pin,
    isChild = isChild,
    createdAt = createdAt,
)

fun Profile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    pin = pin,
    isChild = isChild,
    createdAt = createdAt,
)
