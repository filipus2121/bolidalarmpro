# Инструменты разработчика

* `generate_textures.py` — генерирует все PNG (нужен Pillow и кириллический TTF,
  например DejaVu Sans). Готовые текстуры уже лежат в `src/main/resources`, игрокам Python не нужен.
* `generate_lang.py` — генерирует `ru_RU.lang` / `en_US.lang`.
* `validate_sources.py` — парсит все `.java` через `javalang` (или делает fallback-проверку
  скобок) и проверяет, что пакет `sim` не зависит от Minecraft/Forge.
* `run_sim_test.sh` — компилирует только чистое ядро и запускает 39 проверок
  `SimSelfTest` системным `javac`.

Дополнительно в песочнице была сделана полная компиляция всех 41 исходника через ECJ
с минимальными заглушками API Minecraft/Forge 1.7.10, так как Maven-хосты Forge/Mojang
здесь недоступны. Для реальной сборки используйте `build.gradle` с JDK 8 + Gradle 2.3.
