package com.hotfix.avenlo.domain.repository

import com.hotfix.avenlo.domain.model.IdeaCard

/** 搜索（S4）—— 阻断项 #2 定稿：GET /ideas?query=&tag=&range= */
interface SearchRepository {
    data class Query(val text: String = "", val tag: String? = null, val range: Range = Range.ALL) {
        enum class Range { ALL, TODAY, WEEK, FAVORITE }
    }
    data class History(val text: String, val time: String, val tags: List<String>)
    suspend fun search(q: Query): Result<List<IdeaCard>>
    suspend fun recentSearches(): List<History>
    suspend fun clearHistory()
}
