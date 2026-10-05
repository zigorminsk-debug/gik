# Gik

Android-клиент для запуска генерации Android-проектов в GitHub Actions. Пользователь задаёт репозиторий, токен и описание продукта; приложение вызывает workflow dispatch, после чего workflow генерирует код, автоматически коммитит результат в репозиторий, а push запускает сборку APK как artifact/Release.

## Быстрый старт

1. Откройте проект в Android Studio Ladybug или новее.
2. В целевом репозитории добавьте workflow из [`docs/gik.yml`](docs/gik.yml).
3. Создайте fine-grained GitHub token только для этого репозитория: **Actions: Read and write**, **Contents: Read and write** (для генератора, который коммитит код).
4. В приложении укажите `owner/repo`, токен и техническое задание.

Приложение не отправляет токен никуда, кроме GitHub API, и не сохраняет его. Для production рекомендуется заменить ручной token на GitHub Device Flow через OAuth App и хранить refresh-токен в Android Keystore.

## Архитектура

- `MainActivity.kt` — Compose UI и состояние экрана.
- `GithubApi.kt` — вызов `workflow_dispatch` через GitHub REST API.
- `docs/gik.yml` — безопасный пример pipeline: принимает `task`, вызывает внешний генератор из вашего доверенного репозитория, затем собирает APK.

## Arena.ai

Генератор подключается в GitHub Actions через официальный API Arena.ai. В настройках репозитория добавьте secrets `ARENA_API_URL` и `ARENA_API_KEY`. Workflow вызывает адаптер `docs/scripts/arena_generate.py`, который ожидает ответ `{ "files": [{ "path": "app/...", "content": "..." }] }`.

Логин и пароль от arena.ai нельзя безопасно встраивать в Android-приложение или передавать в workflow: это приведёт к утечке учётных данных и требует автоматизации веб-формы, а не официальной интеграции. Если Arena предоставляет OAuth/device flow или другой официальный API-контракт, его следует использовать вместо пароля; адаптер можно изменить под опубликованный формат API. Секреты провайдера хранятся только в GitHub Actions Secrets, а не в APK.
