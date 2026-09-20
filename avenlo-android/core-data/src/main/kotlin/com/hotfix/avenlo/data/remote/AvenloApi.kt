package com.hotfix.avenlo.data.remote

import com.hotfix.avenlo.domain.model.IdeaCard
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

/** Avenlo API client —— 契约见 mock-server/API_CONTRACT.md（Idea Card V2.1） */
class AvenloApi(private val baseUrl: String) {

    val http: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /** GET /ideas —— 支持 query / tag / range 搜索参数（阻断项 #2 定稿） */
    suspend fun listIdeas(query: String? = null, tag: String? = null, range: String? = null): List<IdeaCard> =
        http.get("$baseUrl/ideas") {
            query?.takeIf { it.isNotBlank() }?.let { parameter("query", it) }
            tag?.let { parameter("tag", it) }
            range?.let { parameter("range", it) }
        }.body()

    suspend fun getIdea(id: String): IdeaCard = http.get("$baseUrl/ideas/$id").body()

    /** POST /captures —— 触发事件。audioUrl 优先传 server 相对路径（先 uploadAudio），本机路径仅 Demo 兜底 */
    suspend fun submitCapture(req: CaptureRequest): CaptureResponse =
        http.post("$baseUrl/captures") {
            // Ktor 2.x：请求侧必须显式声明 Content-Type，ContentNegotiation 才会走 JSON 序列化
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    /** POST /captures/audio —— multipart 音频直传（真机链路：STT 在 server 侧读文件） */
    suspend fun uploadAudio(file: File): AudioUploadResponse {
        val bytes = file.readBytes()
        return http.post("$baseUrl/captures/audio") {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        "file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, "audio/mp4")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        }
                    )
                }
            ))
        }.body()
    }

    suspend fun confirmIdea(id: String) {
        http.post("$baseUrl/ideas/$id/confirm")
    }

    suspend fun deleteIdea(id: String) {
        http.delete("$baseUrl/ideas/$id")
    }

    suspend fun feedbackRelation(id: String, relatedId: String, good: Boolean) {
        http.post("$baseUrl/ideas/$id/feedback") {
            parameter("related_id", relatedId)
            parameter("good", good)
        }
    }

    @kotlinx.serialization.Serializable
    data class CaptureRequest(
        val ts: Long,
        val durationMs: Long,
        val gestures: List<Gesture> = emptyList(),
        val audioUrl: String? = null,
    ) {
        @kotlinx.serialization.Serializable
        data class Gesture(val t: Long, val ev: String)
    }

    @kotlinx.serialization.Serializable
    data class CaptureResponse(val ideaId: String, val status: String)

    @kotlinx.serialization.Serializable
    data class AudioUploadResponse(val audioUrl: String, val size: Long)
}
