package com.hotfix.avenlo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hotfix.avenlo.app.stt.SttEngine
import com.hotfix.avenlo.app.ui.navigation.AvenloApp
import com.hotfix.avenlo.app.ui.theme.AvenloTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application)
        // STT 引擎后台预热（进程级单例，首次 ~15s 模型加载；不阻塞 UI，失败静默降级）
        // 延迟 4s：避开冷启动 JIT/首帧渲染高峰（实测启动后 5s 内点 FAB 会因 CPU 争抢丢手势；
        // ONNX 原生线程不继承 Java 优先级，只能错峰）。transcribeFile 有惰性预热兜底，
        // 用户提前录音也只是转写稍晚，不会失败。
        sttPrewarmScope.launch(Dispatchers.Default) {
            delay(4_000)
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            SttEngine.get().prewarm(application)
        }
        setContent {
            AvenloTheme {
                AvenloApp()
            }
        }
    }
}

/** 预热专用协程作用域：App 级生命周期，不随 Activity 重建而取消 */
private val sttPrewarmScope = kotlinx.coroutines.CoroutineScope(
    kotlinx.coroutines.SupervisorJob() + Dispatchers.Default,
)
