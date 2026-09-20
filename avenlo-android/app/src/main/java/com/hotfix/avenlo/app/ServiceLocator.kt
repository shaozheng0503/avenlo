package com.hotfix.avenlo.app

import android.app.Application
import com.hotfix.avenlo.data.mock.SeedData
import com.hotfix.avenlo.data.remote.AvenloApi
import com.hotfix.avenlo.data.repository.IdeaRepositoryImpl

/** 手写 ServiceLocator（Demo 阶段不引 Hilt，减少构建面） */
object ServiceLocator {
    lateinit var api: AvenloApi
        private set
    lateinit var ideaRepo: IdeaRepositoryImpl
        private set

    fun init(app: Application) {
        api = AvenloApi(BuildConfig.API_BASE_URL)
        ideaRepo = IdeaRepositoryImpl(api, seed = SeedData.ideas)
    }
}
