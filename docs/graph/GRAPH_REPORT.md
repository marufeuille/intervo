# Graph Report - .  (2026-06-19)

## Corpus Check
- 144 files · ~58,473 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1097 nodes · 1827 edges · 75 communities (59 shown, 16 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 125 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Shared Type & Builder Hub|Shared Type & Builder Hub]]
- [[_COMMUNITY_Context & Compose Primitives|Context & Compose Primitives]]
- [[_COMMUNITY_Docs & Design Concepts Hub|Docs & Design Concepts Hub]]
- [[_COMMUNITY_Intents & StateFlow Hub|Intents & StateFlow Hub]]
- [[_COMMUNITY_ViewModel Layer|ViewModel Layer]]
- [[_COMMUNITY_Companion JSON Serialization|Companion JSON Serialization]]
- [[_COMMUNITY_Health Connect Records & Timer Phase|Health Connect Records & Timer Phase]]
- [[_COMMUNITY_Timer Engine Core|Timer Engine Core]]
- [[_COMMUNITY_Ambient Mode Observer|Ambient Mode Observer]]
- [[_COMMUNITY_Heart Rate Sampling|Heart Rate Sampling]]
- [[_COMMUNITY_Timer State & Free Sets|Timer State & Free Sets]]
- [[_COMMUNITY_PDS Account Settings|PDS Account Settings]]
- [[_COMMUNITY_Companion DAO Layer|Companion DAO Layer]]
- [[_COMMUNITY_Heart Rate Accumulator|Heart Rate Accumulator]]
- [[_COMMUNITY_Companion History JSON|Companion History JSON]]
- [[_COMMUNITY_Companion Plan Models|Companion Plan Models]]
- [[_COMMUNITY_Release & Ops Concepts|Release & Ops Concepts]]
- [[_COMMUNITY_Timer UI Composables|Timer UI Composables]]
- [[_COMMUNITY_Companion History Repository|Companion History Repository]]
- [[_COMMUNITY_App Workout Models & DAO|App Workout Models & DAO]]
- [[_COMMUNITY_HR Records & Exercise|HR Records & Exercise]]
- [[_COMMUNITY_Companion E2E Tests|Companion E2E Tests]]
- [[_COMMUNITY_App Exercise DAO|App Exercise DAO]]
- [[_COMMUNITY_PDS Direct Client|PDS Direct Client]]
- [[_COMMUNITY_Workout History & Free Sets|Workout History & Free Sets]]
- [[_COMMUNITY_Companion History Detail|Companion History Detail]]
- [[_COMMUNITY_Launch Complication Service|Launch Complication Service]]
- [[_COMMUNITY_Tile Model|Tile Model]]
- [[_COMMUNITY_Exercise Edit State|Exercise Edit State]]
- [[_COMMUNITY_Companion Plan DAO|Companion Plan DAO]]
- [[_COMMUNITY_Wear Sync (DataEvents)|Wear Sync (DataEvents)]]
- [[_COMMUNITY_Health Connect Writer|Health Connect Writer]]
- [[_COMMUNITY_Workout Plan Sync Client|Workout Plan Sync Client]]
- [[_COMMUNITY_Timer State Machine|Timer State Machine]]
- [[_COMMUNITY_Workout Detail Screen UI|Workout Detail Screen UI]]
- [[_COMMUNITY_Exercise Domain Logic|Exercise Domain Logic]]
- [[_COMMUNITY_Heart Rate Manager|Heart Rate Manager]]
- [[_COMMUNITY_Debug History Receiver|Debug History Receiver]]
- [[_COMMUNITY_PDS Record Mapper Tests|PDS Record Mapper Tests]]
- [[_COMMUNITY_Speech Manager (TTS)|Speech Manager (TTS)]]
- [[_COMMUNITY_Sync Worker|Sync Worker]]
- [[_COMMUNITY_Workout Detail UI (Scrolled)|Workout Detail UI (Scrolled)]]
- [[_COMMUNITY_Exercise Timer Screen UI|Exercise Timer Screen UI]]
- [[_COMMUNITY_History List Screen UI|History List Screen UI]]
- [[_COMMUNITY_Workout Select Screen UI|Workout Select Screen UI]]
- [[_COMMUNITY_Vibration Manager|Vibration Manager]]
- [[_COMMUNITY_Workout Detail UI Model|Workout Detail UI Model]]
- [[_COMMUNITY_After-Wake Select Screen UI|After-Wake Select Screen UI]]
- [[_COMMUNITY_Rest Interval Screen UI|Rest Interval Screen UI]]
- [[_COMMUNITY_Release Notes Validator|Release Notes Validator]]
- [[_COMMUNITY_App Gradle Versioning|App Gradle Versioning]]
- [[_COMMUNITY_Companion Application|Companion Application]]
- [[_COMMUNITY_App Database & Defaults|App Database & Defaults]]
- [[_COMMUNITY_Companion Gradle Versioning|Companion Gradle Versioning]]
- [[_COMMUNITY_Companion Plan Entities|Companion Plan Entities]]
- [[_COMMUNITY_PDS Credentials Store Tests|PDS Credentials Store Tests]]
- [[_COMMUNITY_Completion Screen UI|Completion Screen UI]]
- [[_COMMUNITY_Companion DI Container|Companion DI Container]]
- [[_COMMUNITY_Exercise HR Record|Exercise HR Record]]
- [[_COMMUNITY_Free Set Record|Free Set Record]]
- [[_COMMUNITY_Workout Entity|Workout Entity]]
- [[_COMMUNITY_Workout History Entity|Workout History Entity]]
- [[_COMMUNITY_Ambient StateFlow (1.7.3)|Ambient StateFlow (1.7.3)]]
- [[_COMMUNITY_Bluesky Share Sheet (1.9.4)|Bluesky Share Sheet (1.9.4)]]
- [[_COMMUNITY_Watch Deploy Skill|Watch Deploy Skill]]
- [[_COMMUNITY_wearCompose Hold (1.7.4)|wearCompose Hold (1.7.4)]]
- [[_COMMUNITY_Companion History Entity|Companion History Entity]]
- [[_COMMUNITY_Exercise Category|Exercise Category]]
- [[_COMMUNITY_Performed Set Record Input|Performed Set Record Input]]
- [[_COMMUNITY_1.5.3 Release Notes|1.5.3 Release Notes]]
- [[_COMMUNITY_1.5.4 Release Notes|1.5.4 Release Notes]]

## God Nodes (most connected - your core abstractions)
1. `TimerService` - 44 edges
2. `TimerEngineTest` - 33 edges
3. `WorkoutRepository` - 25 edges
4. `SessionPostDraftComposer` - 21 edges
5. `CompanionRepository` - 21 edges
6. `TimerViewModel` - 19 edges
7. `HrAccumulator` - 18 edges
8. `AppNavigation()` - 18 edges
9. `WorkoutPdsRecordMapper` - 18 edges
10. `TimerEngine` - 17 edges

## Surprising Connections (you probably didn't know these)
- `AGENTS.md (project instructions copy)` --semantically_similar_to--> `CLAUDE.md (project instructions)`  [EXTRACTED] [semantically similar]
  AGENTS.md → CLAUDE.md
- `ci.yml instrumented-test-companion job (phone E2E)` --references--> `companion module (phone app)`  [EXTRACTED]
  .github/workflows/ci.yml → CLAUDE.md
- `companion module (phone app)` --implements--> `Health Connect integration`  [INFERRED]
  CLAUDE.md → docs/privacy-policy.html
- `companion module (phone app)` --implements--> `PDS direct write (XRPC putRecord)`  [INFERRED]
  CLAUDE.md → docs/build.md
- `cut-release skill` --references--> `Tag-driven CI/CD release`  [EXTRACTED]
  .claude/skills/cut-release/SKILL.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Two-layer CI + release pre-gate pipeline** — workflows_ci_build_job, workflows_ci_instrumented_test_job, workflows_release_e2e_job, workflows_release_release_job, concept_two_layer_ci [EXTRACTED 0.95]
- **Tag-driven release to Play internal testing** — workflows_release, workflows_release_e2e_job, workflows_release_e2e_companion_job, workflows_release_release_job, concept_form_factor_tracks, concept_versioncode_autoassign, concept_release_notes_format [EXTRACTED 0.95]
- **Companion PDS direct write data flow** — concept_companion_module, concept_pds_direct_write, concept_workout_plan_record, concept_workout_checkin_record, concept_keystore_encryption, concept_pds_manual_sync [INFERRED 0.85]
- **cut-release release pipeline (notes-writer -> template -> e2e gate -> tag push)** — skills_cut_release_skill, agents_release_notes_writer, docs_release_notes_template, concept_release_gate_e2e, concept_tag_driven_release [EXTRACTED 0.95]
- **PDS sync feature evolution (1.9.0 direct -> 1.9.3 hr-excluded -> 1.10.0 plan/checkin)** — concept_pds_direct_sync, concept_heart_rate_excluded_pds, concept_pds_plan_checkin [INFERRED 0.85]
- **Shared versioning concepts across all release notes (autoversioning + formfactor tracks)** — concept_autoversioning, concept_formfactor_tracks, docs_release_notes_1_9_0, docs_release_notes_1_10_0 [INFERRED 0.75]
- **Workout Select Navigation Flow (start / add / history)** — screenshots_01_workout_select_screen, screenshots_01_workout_select_workout_list, screenshots_01_workout_select_add_button, screenshots_01_workout_select_history_button [EXTRACTED 0.95]
- **After Wake Screen Composition (list item + add button + section title)** — screenshots_02_after_wake_screen, screenshots_02_after_wake_workout_list_item, screenshots_02_after_wake_add_button, screenshots_02_after_wake_section_title [EXTRACTED 1.00]
- **Exercise Management Flow (add/reorder/delete within workout)** — screenshots_02_workout_detail_add_chip, screenshots_02_workout_detail_reorder_chip, screenshots_02_workout_detail_delete_dialog, screenshots_02_workout_detail_exercise_row [INFERRED 0.95]
- **Workout Launch Flow (review exercises then start)** — screenshots_02_workout_detail_exercise_list, screenshots_02_workout_detail_start_button, screenshots_02_workout_detail_screen [INFERRED 0.85]
- **Workout Plan Management Flow (view list, edit item, add, start)** — screenshots_02b_workout_detail_scroll_screen, screenshots_02b_workout_detail_scroll_exercise_list, screenshots_02b_workout_detail_scroll_add_button, screenshots_02b_workout_detail_scroll_start_button [INFERRED 0.85]
- **Exercise List Item Composition (name, set/rep summary, edit chevron)** — screenshots_02b_workout_detail_scroll_exercise_item, screenshots_02b_workout_detail_scroll_edit_nav, screenshots_02b_workout_detail_scroll_exercise_list [INFERRED 0.85]
- **Active Exercise Interval Display** — screenshots_03_timer_exercise_countdown_ring, screenshots_03_timer_exercise_time_display, screenshots_03_timer_exercise_current_exercise, screenshots_03_timer_exercise_status_label [EXTRACTED 1.00]
- **Workout Progress Tracking Pattern** — screenshots_03_timer_exercise_countdown_ring, screenshots_03_timer_exercise_set_progress, screenshots_03_timer_exercise_time_display [INFERRED 0.85]
- **Wear OS Glanceable Centered Layout** — screenshots_03_timer_exercise_screen, screenshots_03_timer_exercise_color_scheme, screenshots_03_timer_exercise_countdown_ring [INFERRED 0.85]
- **Rest Interval Transition Flow** — screenshots_04_timer_rest_countdown, screenshots_04_timer_rest_next_up, screenshots_04_timer_rest_skip_button [INFERRED 0.85]
- **Completion Screen Components** — screenshots_05_completion_screen, screenshots_05_completion_summary_stats, screenshots_05_completion_done_action [EXTRACTED 0.95]
- **History Entry Composition** — screenshots_06_history_list_entries, screenshots_06_history_entry_date, screenshots_06_history_entry_summary [EXTRACTED 1.00]
- **History-to-Detail Navigation Flow** — screenshots_06_history_list_entries, screenshots_06_history_detail_navigation, screenshots_06_history_back_nav [INFERRED 0.75]

## Communities (75 total, 16 thin omitted)

### Community 0 - "Shared Type & Builder Hub"
Cohesion: 0.06
Nodes (62): Boolean, Int, Modifier, String, Int, Long, String, ExerciseDetail (+54 more)

### Community 1 - "Context & Compose Primitives"
Cohesion: 0.06
Nodes (44): Context, Int, List, String, Boolean, Color, ExerciseMode, String (+36 more)

### Community 2 - "Docs & Design Concepts Hub"
Cohesion: 0.05
Nodes (56): Health Connect activelyRecorded metadata fix, Ambient Mode (always-on display), app module (Wear OS app), scripts/check_release_notes.py (release note validator), CompanionE2ETest (phone instrumented test), companion module (phone app), DebugWorkoutHistoryReceiver (debug-only seed), Form-factor-specific Play tracks (wear:internal / internal) (+48 more)

### Community 3 - "Intents & StateFlow Hub"
Cohesion: 0.08
Nodes (21): Boolean, Int, Intent, Long, StateFlow, String, TimerSnapshot, TimerState (+13 more)

### Community 4 - "ViewModel Layer"
Cohesion: 0.05
Nodes (23): AndroidViewModel, Boolean, Int, Pair, StateFlow, String, TimerState, Boolean (+15 more)

### Community 5 - "Companion JSON Serialization"
Cohesion: 0.10
Nodes (19): Boolean, CompanionWorkoutHistory, Int, JsonArray, JsonObject, List, Map, String (+11 more)

### Community 6 - "Health Connect Records & Timer Phase"
Cohesion: 0.11
Nodes (18): Exercise, FreeSetRecordInput, JSONObject, PerformedSetRecordInput, TimerPhase, ByteArray, Boolean, String (+10 more)

### Community 7 - "Timer Engine Core"
Cohesion: 0.13
Nodes (6): Exercise, ExerciseMode, Int, String, TimerState, TimerEngineTest

### Community 8 - "Ambient Mode Observer"
Cohesion: 0.08
Nodes (23): AmbientLifecycleObserver, Boolean, Int, Long, String, Bundle, Boolean, Int (+15 more)

### Community 9 - "Heart Rate Sampling"
Cohesion: 0.13
Nodes (17): Exercise, ExerciseHrInput, ExerciseMode, Flow, FreeSetRecordInput, HrSample, Int, List (+9 more)

### Community 10 - "Timer State & Free Sets"
Cohesion: 0.25
Nodes (13): Boolean, Exercise, Int, List, PerformedSetRecordInput, TimerPhase, TimerState, Speak (+5 more)

### Community 11 - "PDS Account Settings"
Cohesion: 0.11
Nodes (17): StateFlow, StateFlow, Boolean, Int, PdsAccountSettings, StateFlow, String, HistoryDetailViewModel (+9 more)

### Community 12 - "Companion DAO Layer"
Cohesion: 0.09
Nodes (18): Context, ExerciseMode, String, Context, CompanionWorkoutHistoryDao, CompanionWorkoutPlanDao, AppDatabase, buildDatabase() (+10 more)

### Community 13 - "Heart Rate Accumulator"
Cohesion: 0.12
Nodes (9): ExerciseHrInput, Int, List, Long, String, ExerciseHrAccum, HrAccumulator, HrSample (+1 more)

### Community 14 - "Companion History JSON"
Cohesion: 0.19
Nodes (9): Boolean, CompanionWorkoutHistory, Int, JsonArray, JsonObject, List, String, PdsRecordRef (+1 more)

### Community 15 - "Companion Plan Models"
Cohesion: 0.14
Nodes (11): Boolean, CompanionPlanExercise, CompanionWorkoutHistory, CompanionWorkoutPlan, CompanionWorkoutPlanWithExerciseCount, Flow, Int, List (+3 more)

### Community 16 - "Release & Ops Concepts"
Cohesion: 0.14
Nodes (20): ci-triage agent, release-notes-writer agent, semver-based versionCode auto-numbering, Removal of BigQuery/Firebase external sync, Form-factor-specific Play tracks (wear:internal / internal), Heart-rate excluded from PDS payload, PDS direct sync (App Password auth, putRecord), PDS plan/checkin record separation (+12 more)

### Community 17 - "Timer UI Composables"
Cohesion: 0.17
Nodes (18): Color, Modifier, TimerState, Boolean, Int, String, ActiveTimerContent(), AmbientTimerContent() (+10 more)

### Community 18 - "Companion History Repository"
Cohesion: 0.21
Nodes (7): CompanionWorkoutHistory, Flow, Int, List, Long, String, CompanionWorkoutHistoryDao

### Community 19 - "App Workout Models & DAO"
Cohesion: 0.20
Nodes (7): Flow, Int, List, String, Workout, WorkoutWithCount, WorkoutDao

### Community 20 - "HR Records & Exercise"
Cohesion: 0.18
Nodes (14): Exercise, ExerciseHrInput, HrSample, Int, List, PerformedSetRecordInput, String, T (+6 more)

### Community 21 - "Companion E2E Tests"
Cohesion: 0.12
Nodes (11): awaitText(), CompanionE2ETest, launchApp(), openTab(), MainActivity, Boolean, Long, String (+3 more)

### Community 22 - "App Exercise DAO"
Cohesion: 0.23
Nodes (7): Exercise, Flow, Int, List, String, ExerciseDao, WorkoutExerciseCount

### Community 23 - "PDS Direct Client"
Cohesion: 0.31
Nodes (9): Boolean, CompanionWorkoutHistory, JsonObject, String, PdsDirectClient, Session, PdsCredentials, PdsRecordRef (+1 more)

### Community 24 - "Workout History & Free Sets"
Cohesion: 0.21
Nodes (8): Flow, FreeSetRecord, List, String, WorkoutHistory, WorkoutHistoryWithFreeSetRecords, WorkoutHistoryDao, ExerciseHrRecord

### Community 25 - "Companion History Detail"
Cohesion: 0.22
Nodes (9): CompanionWorkoutHistory, ExerciseDetail, Int, List, Map, Pair, String, WorkoutDetailUiModel (+1 more)

### Community 26 - "Launch Complication Service"
Cohesion: 0.20
Nodes (8): Int, LaunchComplicationService, ComplicationData, ComplicationDataSourceService, ComplicationRequest, ComplicationRequestListener, ComplicationType, PendingIntent

### Community 27 - "Tile Model"
Cohesion: 0.20
Nodes (8): Int, List, WorkoutWithCount, Int, String, TileWorkout, tileWorkouts(), TileModelTest

### Community 28 - "Exercise Edit State"
Cohesion: 0.25
Nodes (11): Boolean, Exercise, Int, List, StateFlow, String, Workout, ExerciseRow() (+3 more)

### Community 29 - "Companion Plan DAO"
Cohesion: 0.25
Nodes (7): CompanionPlanExercise, CompanionWorkoutPlan, CompanionWorkoutPlanWithExerciseCount, Flow, List, String, CompanionWorkoutPlanDao

### Community 30 - "Wear Sync (DataEvents)"
Cohesion: 0.19
Nodes (12): CompanionPlanExercise, CompanionWorkoutHistory, List, Long, String, DataEventBuffer, parsePlanExercises(), PlanSnapshot (+4 more)

### Community 31 - "Health Connect Writer"
Cohesion: 0.23
Nodes (8): Boolean, CompanionWorkoutHistory, Int, List, String, HealthConnectWriter, ParsedSample, HealthConnectClient

### Community 32 - "Workout Plan Sync Client"
Cohesion: 0.24
Nodes (7): Exercise, List, String, T, Workout, toPlanExercisesJson(), WorkoutPlanSyncClient

### Community 33 - "Timer State Machine"
Cohesion: 0.31
Nodes (9): Exercise, Int, Complete, ExercisePhase, Idle, RepRestPhase, RestPhase, TimerPhase (+1 more)

### Community 34 - "Workout Detail Screen UI"
Cohesion: 0.27
Nodes (10): Add Exercise Chip (＋ 追加), Confirm Delete Dialog (long-press exercise), Edit Workout Chip (✎ 編集), Exercise List (ScalingLazyColumn rows), Exercise Row (name, duration/sets/rest summary), Reorder Toggle Chip (↕ 並び替え / ✓ 完了), Workout Detail Screen, Start Workout Button (▶ スタート, orange) (+2 more)

### Community 35 - "Exercise Domain Logic"
Cohesion: 0.28
Nodes (8): Boolean, Int, effectiveRepsPerSet(), Exercise, ExerciseMode, isDurationUnlimited(), isOpenEndedReps(), WorkoutExerciseCount

### Community 36 - "Heart Rate Manager"
Cohesion: 0.25
Nodes (6): Int, StateFlow, String, Throwable, debugLog(), HeartRateManager

### Community 37 - "Debug History Receiver"
Cohesion: 0.25
Nodes (6): BroadcastReceiver, CompanionWorkoutHistory, Context, Intent, String, DebugWorkoutHistoryReceiver

### Community 38 - "PDS Record Mapper Tests"
Cohesion: 0.39
Nodes (3): CompanionWorkoutHistory, WorkoutPdsRecordMapperTest, WorkoutPdsRecordMapper

### Community 39 - "Speech Manager (TTS)"
Cohesion: 0.25
Nodes (4): Int, String, TextToSpeech, SpeechManager

### Community 40 - "Sync Worker"
Cohesion: 0.29
Nodes (5): Context, CoroutineWorker, Result, enqueue(), SyncWorker

### Community 41 - "Workout Detail UI (Scrolled)"
Cohesion: 0.33
Nodes (7): Add Exercise Button (追加, dark gray + orange plus), Wear OS Dark Theme Layout, Exercise Edit Navigation (chevron per item), Exercise List Item (with set/rep summary + chevron), Scrollable Exercise List, Workout Detail Screen (Scrolled Variant), Start Workout Button (スタート, orange play)

### Community 42 - "Exercise Timer Screen UI"
Cohesion: 0.43
Nodes (7): High-Contrast Color Scheme (black bg, orange/white), Circular Countdown Ring (orange progress indicator), Current Exercise Name (Squat / スクワット), Active Exercise Timer Screen, Set Progress Indicator (1 / 3 sets), Exercise Status Label (運動中 / in motion), Remaining Time Display (35 seconds)

### Community 43 - "History List Screen UI"
Cohesion: 0.29
Nodes (7): Back Navigation (up affordance), Tap-to-Open Detail Flow, Empty/Placeholder Handling Pattern, Per-Entry Completion Date, Per-Entry Summary Stats (workout name, duration, calories), Scrollable History List Entries, Workout History List Screen

### Community 44 - "Workout Select Screen UI"
Cohesion: 0.53
Nodes (6): Add Workout Button (+ 追加), Wear OS Dark Theme Design, History Button (履歴), Japanese Localization (ワークアウト / いつもの / 履歴), Workout Select Screen, Saved Workout List Item (いつもの / 2種目)

### Community 45 - "Vibration Manager"
Cohesion: 0.33
Nodes (4): VibratePattern, VibrationManager, VibratePattern, Vibrator

### Community 46 - "Workout Detail UI Model"
Cohesion: 0.40
Nodes (4): Boolean, ExerciseDetail, ExerciseModeUi, WorkoutDetailUiModel

### Community 47 - "After-Wake Select Screen UI"
Cohesion: 0.50
Nodes (5): Add Button (追加 / orange), Dark Theme Black Background, After Wake Screen (Wear OS Workout Select), Section Title ワーカアウト (Workout), Workout List Item (いつもの / 2種目)

### Community 48 - "Rest Interval Screen UI"
Cohesion: 0.50
Nodes (5): Rest Countdown Display, Next-Up Exercise Preview, Circular Progress Ring, Timer Rest Interval Screen, Skip Rest Control

### Community 49 - "Release Notes Validator"
Cohesion: 0.60
Nodes (4): check(), extract_whatsnew(), main(), 見出し以降の最初のコードフェンス内テキストを返す。無ければ None。

### Community 50 - "App Gradle Versioning"
Cohesion: 0.50
Nodes (3): Int, String, versionCodeFrom()

### Community 51 - "Companion Application"
Cohesion: 0.50
Nodes (3): AppContainer, Application, CompanionApplication

### Community 53 - "Companion Gradle Versioning"
Cohesion: 0.50
Nodes (3): Int, String, versionCodeFrom()

### Community 54 - "Companion Plan Entities"
Cohesion: 0.50
Nodes (3): CompanionPlanExercise, CompanionWorkoutPlan, CompanionWorkoutPlanWithExerciseCount

### Community 56 - "Completion Screen UI"
Cohesion: 0.83
Nodes (4): Done/Finish Action, Post-Workout Completion Flow, Workout Completion Screen, Completion Summary Stats

## Knowledge Gaps
- **253 isolated node(s):** `String`, `Int`, `String`, `Boolean`, `Long` (+248 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AppNavigation()` connect `Ambient Mode Observer` to `Context & Compose Primitives`, `ViewModel Layer`, `Exercise Edit State`, `Timer UI Composables`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Why does `MainActivity` connect `Ambient Mode Observer` to `Companion E2E Tests`?**
  _High betweenness centrality (0.035) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `SessionPostDraftComposer` (e.g. with `.composesPostWithEveryExerciseLine()` and `.splitsLongPostWithoutDroppingExercises()`) actually correct?**
  _`SessionPostDraftComposer` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `String`, `Int`, `String` to the rest of the system?**
  _260 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Shared Type & Builder Hub` be split into smaller, more focused modules?**
  _Cohesion score 0.057902973395931145 - nodes in this community are weakly interconnected._
- **Should `Context & Compose Primitives` be split into smaller, more focused modules?**
  _Cohesion score 0.05593220338983051 - nodes in this community are weakly interconnected._
- **Should `Docs & Design Concepts Hub` be split into smaller, more focused modules?**
  _Cohesion score 0.05454545454545454 - nodes in this community are weakly interconnected._