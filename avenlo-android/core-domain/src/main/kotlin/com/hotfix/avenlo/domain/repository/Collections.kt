package com.hotfix.avenlo.domain.repository

import com.hotfix.avenlo.domain.model.Collection
import com.hotfix.avenlo.domain.model.DailyReview

interface CollectionRepository {
    suspend fun getCollections(): Result<List<Collection>>
    /** 阻断项 #2 定稿：POST /collections 手动新建 */
    suspend fun createCollection(name: String): Result<Collection>
}

interface ReviewRepository {
    suspend fun getDailyReview(): Result<DailyReview>
}
