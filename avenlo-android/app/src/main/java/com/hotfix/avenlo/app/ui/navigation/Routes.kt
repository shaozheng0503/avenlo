package com.hotfix.avenlo.app.ui.navigation

/** 8 屏路由（S0–S7）+ 底部导航 4 Tab（阻断项 #1 建议默认值，待队友确认） */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val DETAIL = "detail/{ideaId}"
    const val COLLECTIONS = "collections"
    const val SEARCH = "search"
    const val REVIEW = "review"
    const val MINE = "mine"

    fun detail(ideaId: String) = "detail/$ideaId"

    /** 捕捉屏；autoStart=true 时跳过 Ready 态直接请求权限开录（长按 FAB 直达） */
    const val CAPTURE = "capture?autoStart={autoStart}"
    fun capture(autoStart: Boolean = false) = "capture?autoStart=$autoStart"

    /** 底部导航 4 Tab：首页 / 记录 / 统计 / 我的（标签 ⚠️ 源文件损坏，按语义推断） */
    data class Tab(val route: String, val label: String)
    val bottomTabs = listOf(
        Tab(HOME, "首页"),
        Tab(REVIEW, "记录"),
        Tab(REVIEW, "统计"),
        Tab(MINE, "我的"),
    )
}
