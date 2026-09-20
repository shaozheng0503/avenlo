package com.hotfix.avenlo.data.repository

import com.hotfix.avenlo.data.remote.AvenloApi
import com.hotfix.avenlo.domain.model.Collection
import com.hotfix.avenlo.domain.model.DailyReview
import com.hotfix.avenlo.domain.repository.CollectionRepository
import com.hotfix.avenlo.domain.repository.ReviewRepository

/** 灵感集 Repo：server 优先，断网回落本地种子 */
class CollectionsRepositoryImpl(
    private val api: AvenloApi,
    private val seed: List<Collection> = emptyList(),
) : CollectionRepository {

    override suspend fun getCollections(): Result<List<Collection>> =
        runCatching { api.listCollections() }
            .recoverCatching { seed }

    override suspend fun createCollection(name: String): Result<Collection> =
        runCatching { api.createCollection(name) }
}

/** 今日回顾 Repo：server 优先，断网回落本地种子 */
class ReviewRepositoryImpl(
    private val api: AvenloApi,
    private val seed: DailyReview? = null,
) : ReviewRepository {

    override suspend fun getDailyReview(): Result<DailyReview> =
        runCatching { api.getDailyReview() }
            .recoverCatching { seed ?: error("no seed review and server unreachable") }
}
