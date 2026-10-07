package com.avitrisphare.whiteboard.domain.model

import java.util.UUID

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
