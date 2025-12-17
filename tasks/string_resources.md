# Task: String Resources Consolidation

Migrate hardcoded UI strings to `strings.xml` for i18n readiness.

## Status: Complete ✓

## Problem

Hardcoded strings in Compose UI are not translatable. Examples:
- `"Running"`, `"Aerobic"`, `"Swimming"` (duplicated - already in strings.xml)
- `"+5 min"`, `"+15 min"`, `"+1 h"`
- `"Settings"`, `"Daily Goal"`, `"Done"`
- `"Cancel"`, `"Clear"`, `"OK"`
- `"Today's progress"`, `"Goal reached!"`
- Calendar day names: `"Su"`, `"Mo"`, etc.

## Goal

All user-facing strings should use `stringResource(R.string.xxx)` to enable future localization.

## Implementation Checklist

### Phase 1: Audit
- [x] List all hardcoded strings in Compose files
- [x] Identify which already exist in `strings.xml`
- [x] Identify new strings needed

### Phase 2: Add Missing Strings
- [x] Add new entries to `res/values/strings.xml`
- [x] Use proper naming convention: `screen_component_description`
- [x] Handle plurals where needed (`plurals` resource)
- [x] Handle parameterized strings (`%d`, `%s`)

### Phase 3: Migrate Code
- [x] `MainScreen.kt` - progress display, buttons, panels
- [x] `CalendarView.kt` - day names, legend labels
- [x] `NotificationHelper.kt` - already uses strings.xml (verified)
- [x] Any other UI files

### Phase 4: Cleanup
- [x] Remove duplicate string definitions
- [x] Verify no hardcoded user-facing strings remain
- [x] Test UI displays correctly (build successful)

## Files to Modify

| File | Hardcoded Strings |
|------|-------------------|
| `MainScreen.kt` | Settings, progress bar text, input panel, settings panel |
| `CalendarView.kt` | Day abbreviations, legend labels |
| `strings.xml` | Add new entries |

## String Naming Convention

```xml
<!-- Screen_Component_Description -->
<string name="main_settings_title">Settings</string>
<string name="main_progress_goal_reached">Goal reached!</string>
<string name="input_duration_5min">+5 min</string>
<string name="calendar_day_sunday">Su</string>
```

## Notes

- Keep `strings.xml` organized by screen/feature
- Consider plural forms for languages that need them
- Some strings like emoji (🏃, 🔥) don't need localization
- Format strings: `<string name="progress_format">%1$d of %2$d min</string>`
