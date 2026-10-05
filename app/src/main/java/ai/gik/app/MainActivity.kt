package ai.gik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GikApp() } }
}

class GikViewModel : ViewModel() {
    var token by mutableStateOf(""); var repository by mutableStateOf(""); var task by mutableStateOf("")
    var status by mutableStateOf("Готово к запуску"); var busy by mutableStateOf(false)
    private val api = GithubApi()
    fun run() { if (token.isBlank() || repository.isBlank() || task.isBlank()) { status = "Заполните все поля"; return }
        val parts = repository.trim().removePrefix("https://github.com/").removeSuffix("/").split("/")
        if (parts.size < 2) { status = "Укажите репозиторий в формате owner/repo"; return }
        viewModelScope.launch { busy = true; status = "Отправляем задание в GitHub Actions…"
            try { api.dispatch(token, parts[0], parts[1], task); status = "Сборка запущена. Откройте Actions в GitHub, чтобы следить за прогрессом." }
            catch (e: Exception) { status = "Ошибка: ${e.message ?: "проверьте токен и репозиторий"}" }
            finally { busy = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GikApp() {
    val vm = remember { GikViewModel() }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF6750A4))) {
        Scaffold(topBar = { TopAppBar(title = { Text("Gik · Android builder") }) }) { pad ->
            Column(Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Соберите приложение по описанию", style = MaterialTheme.typography.headlineSmall)
                Text("Gik создаёт задание для вашего репозитория, а GitHub Actions генерирует код и собирает APK.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(vm.repository, { vm.repository = it }, Modifier.fillMaxWidth(), label = { Text("GitHub repository") }, placeholder = { Text("owner/android-project") }, singleLine = true)
                OutlinedTextField(vm.token, { vm.token = it }, Modifier.fillMaxWidth(), label = { Text("GitHub token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, supportingText = { Text("Токен используется только в памяти и не сохраняется.") })
                OutlinedTextField(vm.task, { vm.task = it }, Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("Что создать") }, placeholder = { Text("Например: приложение заметок с авторизацией…") })
                Button(onClick = vm::run, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (vm.busy) "Запускаем…" else "Создать и собрать") }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Статус", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(6.dp)); Text(vm.status) } }
                Text("Безопасность", style = MaterialTheme.typography.titleMedium)
                Text("Используйте fine-grained token с доступом Contents: Read and write и Actions: Read and write только к нужному репозиторию. В репозитории должен быть workflow .github/workflows/gik.yml с input task.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
