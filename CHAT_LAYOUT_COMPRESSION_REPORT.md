# Chat Layout Compression Report

## Problem

Auf Galaxy A56 nahmen Skill-Dropdown, Role-Selector, Run, Apply und Eingabefeld zu viel vertikale Höhe ein (~5 separate Zeilen). Das große Skill-Dropdown-Menü verdeckte unkontrolliert Bildschirminhalte.

## Geänderte Dateien

- `app/src/main/java/com/pocketcodeagent/ui/chat/ChatPanel.kt` (rewrite)

## Skill BottomSheet

**Vorher:**
- `DropdownMenu` mit `heightIn(max = 340.dp)` → überdeckte willkürlich Inhalte

**Nachher:**
- `ModalBottomSheet` mit `fillMaxHeight(0.7f)` → max 70% Bildschirmhöhe
- Jede Skill-Zeile: Name, Kategorie+Mode, Check-Icon wenn ausgewählt
- "None" als erste Option (ohne Skill)
- Scrollbare Liste über `LazyColumn`

## Role BottomSheet

**Vorher:**
- `DropdownMenu` mit `heightIn(max = 380.dp)` → gleiches Überdeckungsproblem

**Nachher:**
- `ModalBottomSheet` mit `fillMaxHeight(0.7f)`
- Jede Rollen-Zeile: Name, Kurzbeschreibung, Check-Icon wenn ausgewählt
- Scrollbare Liste über `LazyColumn`

## Neue Control-Struktur

| Reihe | Inhalt |
|-------|--------|
| A | Context Chip + "New" Session Button |
| B | Discuss/Build (segmented compact) + Skill Chip |
| C | Role Chip + Run/Stop Button + Apply (nur wenn pending) |
| D | Input-Feld + runder Send Button |

Vertikale Paddings von 4-8dp auf 1-4dp reduziert. Touch-Targets ≥40dp eingehalten.

## Apply Button Logik

- **Vorher**: Immer sichtbar, aber disabled (grau) wenn keine Patches
- **Nachher**: Nur sichtbar, wenn `pendingChanges.isNotEmpty()` → spart Platz

## Prompt Input

- Platzhalter: "Beschreibe, was der Agent bauen soll..."
- Mehrzeilig (`maxLines = 4`), aber Höhe begrenzt: `heightIn(min = 40.dp, max = 100.dp)`
- Send-Button: rund (`CircleShape`), 40dp Durchmesser
- Stop-Button erscheint statt Send wenn Agent läuft

## Galaxy A56 Verbesserungen

- ~35-40% weniger vertikaler Platz für Controls
- Mehr Platz für Chat-Messages (weight(1f))
- Keine Popups, die Inhalte unkontrolliert verdecken
- BottomSheets scrollbar mit übersichtlicher Zeilenstruktur
- Buttons bleiben touch-freundlich (≥40dp)

## Build Ergebnis

```
assembleDebug: BUILD SUCCESSFUL
```

Keine neuen Compiler-Fehler.
