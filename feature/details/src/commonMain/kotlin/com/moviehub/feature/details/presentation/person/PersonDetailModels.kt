package com.moviehub.feature.details.presentation.person

import com.moviehub.core.model.MediaPreview

data class PersonDetailUiState(
    val isLoading: Boolean = true,
    val person: PersonDetail? = null,
    val credits: List<MediaPreview> = emptyList(),
    val error: String? = null,
)

data class PersonDetail(
    val id: Int,
    val name: String,
    val photoUrl: String?,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val knownForDepartment: String?,
    val alsoKnownAs: List<String>? = null,
)
