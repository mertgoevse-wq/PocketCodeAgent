# Performance Fix Report - PocketCodeAgent

## Zusammenfassung der gefundenen Lag-Ursachen

Die Analyse des PocketCodeAgent-Projekts hat mehrere kritische Performance- und Stabilitäts-Engpässe offengelegt, die primär für das Einfrieren der App und das Stottern der UI verantwortlich waren:
1. **Chat Streaming Lags:** Der empfangene Text (`activeStreamingText`) wurde direkt ohne Limit in den Compose-State geschrieben. Ein sehr schnelles API-Streaming (wie z.B. bei GPT-4o oder Claude) führt zu hunderten von Recompositions pro Sekunde.
2. **LazyColumn Stottern:** Die Chat-Liste nutzte `animateScrollToItem` bei jeder Token-Aktualisierung und die Items hatten keine stabilen Keys. Dies führte zu ständigen Neuzeichnungen der kompletten Chat-Bubbles.
3. **Null-Exceptions:** Bei Netzwerkfehlern oder Fehlern innerhalb der Agents wurde oft `exception.message` direkt gelesen, was zu der kryptischen und frustrierenden Meldung `"Error executing PlannerAgent: null"` führte.
4. **Workspace Scan Blockierung:** Der SAF-basierte rekursive Dateiscan über `WorkspaceManager` scannte unbegrenzt tief und erfasste gigantische Ordner wie `node_modules` oder `.git`, was den Thread lahmlegte und riesige Mengen an Dateien lud.
5. **WebView Memory Leaks:** Die Instanzen des WebViews (`LivePreviewScreen`) blieben beim Verlassen des Screens als Zombies im Speicher, da `.destroy()` nicht sauber im Lifecycle verankert war.
6. **SSE Parser Abstürze:** Providers, die kein echtes Streaming (`stream: true`) unterstützen, gaben komplette JSONs zurück. Die `data: ` Suchlogik übersprang diese oder stürzte ab.

---

## Welche Dateien wurden geändert?

- `app/src/main/java/com/pocketcodeagent/ui/viewmodel/AgentViewModel.kt`
- `app/src/main/java/com/pocketcodeagent/ui/screen/ChatAgentScreen.kt`
- `app/src/main/java/com/pocketcodeagent/ui/screen/LivePreviewScreen.kt`
- `app/src/main/java/com/pocketcodeagent/data/local/WorkspaceManager.kt`
- `app/src/main/java/com/pocketcodeagent/data/network/ApiClient.kt`
- `app/src/main/java/com/pocketcodeagent/data/repository/AgentRepository.kt`

---

## Wie wurde das Streaming verbessert?
In `AgentViewModel.kt` wurde eine Puffer-Logik eingeführt (Debouncing / Throttling). Der Zustand `activeStreamingText` wird nun nicht bei jedem Chunk geupdatet, sondern maximal alle 150ms gebündelt in die UI gepusht. Die finale Chat-Nachricht wird erst hinzugefügt, wenn das Streaming den `[DONE]` Status erreicht hat.

In der `LazyColumn` (`ChatAgentScreen.kt`) wurde das rechenintensive `animateScrollToItem` auf das direkte `scrollToItem` geändert. Außerdem erhielten alle Chat-Items einen eindeutigen und stabilen `key = { it.id }`, womit Compose nun Smart-Recompositions durchführen kann, anstatt bei jedem Token die komplette Liste neu aufzubauen.

---

## Wie wurde FileTree-Scanning verbessert?
`WorkspaceManager.kt` führt Dateiscans nun standardmäßig über `Dispatchers.IO` aus, um den Main-Thread komplett zu entlasten. Es wurde eine `maxDepth`-Limitierung (Level 6) sowie eine Blacklist implementiert. Ordner wie `node_modules`, `.git`, `.gradle`, `build`, `dist` und `.idea` werden übersprungen, was die Einlesezeit massiv reduziert.

---

## Wie wurde WebView stabilisiert?
`LivePreviewScreen.kt` nutzt nun Compose `DisposableEffect`. Die WebView-Instanz wird im `onDispose`-Block durch den Aufruf von `webViewInstance?.destroy()` ordnungsgemäß aus dem Speicher gelöscht, wenn der Benutzer zu einem anderen Screen navigiert. Weiterhin wird der WebView über `remember` initialisiert und am Leben gehalten.

---

## Wie wurde der Fehler "Error executing PlannerAgent: null" behoben?
Die `AgentRepository.kt` und `ApiClient.kt` verarbeiten `Exception`s nun deutlich intelligenter. Eine zentrale `sanitizeThrowable`-Logik prüft, ob eine `exception.message` vorhanden ist. Wenn diese leer ist (`null`), greift die Methode auf den Klassennamen (`exception.javaClass.simpleName`) zurück oder gibt "Unbekannter Fehler" aus, damit der Benutzer immer einen Hinweis auf die tatsächliche Fehlerart erhält. Sensible Netzwerk-Daten, Request-Header und API-Keys wurden zudem aus allen Fehlermeldungen herausgefiltert und werden weder angezeigt noch geloggt.

---

## Ergebnis des Builds
Nach all diesen Anpassungen verläuft der Prozess `.\gradlew clean assembleDebug` **erfolgreich**. Es traten keine Regressionen auf.
