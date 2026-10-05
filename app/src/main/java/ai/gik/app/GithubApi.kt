package ai.gik.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Minimal GitHub REST client. The token is never logged or persisted. */
class GithubApi(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun dispatch(token: String, owner: String, repo: String, task: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("ref", "main").put("inputs", JSONObject().put("task", task)).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("https://api.github.com/repos/$owner/$repo/actions/workflows/gik.yml/dispatches")
            .header("Authorization", "Bearer $token").header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28").post(body).build()
        client.newCall(request).execute().use { response -> if (!response.isSuccessful) throw IllegalStateException(describe(response.code, owner, repo)) }
    }

    private fun describe(code: Int, owner: String, repo: String): String = when (code) {
        401 -> "токен недействителен (401). Проверьте GitHub token."
        403 -> "нет прав (403). Токену нужны Actions: Read and write для $owner/$repo."
        404 -> "не найден workflow gik.yml в $owner/$repo (404). Добавьте .github/workflows/gik.yml в ветку main или проверьте имя репозитория и права токена."
        422 -> "workflow найден, но отклонил запрос (422): нет ветки main или input task."
        else -> "GitHub HTTP $code"
    }
}
