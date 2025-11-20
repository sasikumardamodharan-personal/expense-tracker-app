# Google Sheets Sync Feature Proposal

## Overview
Add Google Sheets integration to enable cloud backup, data export/import, and cross-device synchronization of expense data.

---

## User Benefits

### 🔄 Automatic Backup
- All expenses automatically backed up to Google Sheets
- Never lose data even if phone is lost/damaged
- Access data from any device with Google Sheets

### 📊 Data Analysis
- Use Google Sheets powerful features (pivot tables, charts, formulas)
- Share expense reports with family/accountant
- Create custom views and analysis

### 🔀 Cross-Device Sync
- Use app on multiple devices
- Data stays in sync across all devices
- Edit in Google Sheets, sync back to app

---

## Implementation Approach

### Option 1: Simple CSV Export/Import (Recommended for MVP)
**Pros:**
- ✅ No API keys or OAuth needed
- ✅ Works offline
- ✅ User has full control
- ✅ Can be implemented quickly
- ✅ Works with any spreadsheet app (Excel, Sheets, etc.)

**Cons:**
- ❌ Manual process (not automatic)
- ❌ No real-time sync
- ❌ User must manage files

**Implementation:**
1. Export to CSV button in Settings
2. Import from CSV button in Settings
3. File picker integration
4. CSV format: Date, Amount, Category, Description

**Estimated Time:** 1-2 days

---

### Option 2: Google Sheets API Integration (Full Featured)
**Pros:**
- ✅ Automatic sync
- ✅ Real-time updates
- ✅ Seamless experience
- ✅ Professional solution

**Cons:**
- ❌ Requires Google Cloud project setup
- ❌ OAuth authentication complexity
- ❌ API quota limits
- ❌ Requires internet connection
- ❌ More complex error handling

**Implementation:**
1. Google Cloud Console setup
2. OAuth 2.0 authentication flow
3. Google Sheets API integration
4. Sync service with conflict resolution
5. Background sync worker
6. Sync status UI

**Estimated Time:** 1-2 weeks

---

## Recommended Approach: Phased Implementation

### Phase 1: CSV Export/Import (Quick Win)
Start with simple CSV export/import to provide immediate value:

**Features:**
- Export all expenses to CSV file
- Import expenses from CSV file
- Duplicate detection
- Data validation on import
- Share CSV via any app (Drive, Email, etc.)

**User Flow:**
1. Settings → Export Data → Save CSV file
2. Upload CSV to Google Sheets manually
3. Edit in Google Sheets
4. Download CSV from Google Sheets
5. Settings → Import Data → Select CSV file

### Phase 2: Google Drive Integration (Medium Term)
Add Google Drive integration for easier file management:

**Features:**
- Save exports directly to Google Drive
- Browse and import from Google Drive
- Automatic backup to Drive folder
- Version history

### Phase 3: Google Sheets API (Long Term)
Full bidirectional sync with Google Sheets:

**Features:**
- One-click sync with Google Sheets
- Automatic background sync
- Conflict resolution
- Real-time updates
- Offline support with queue

---

## CSV Export/Import Implementation Plan

### 1. Data Format

**CSV Structure:**
```csv
Date,Amount,Category,Description,Created,Updated
2024-01-15,50.00,Food,Lunch at cafe,2024-01-15 12:30:00,2024-01-15 12:30:00
2024-01-16,25.50,Transport,Uber ride,2024-01-16 08:15:00,2024-01-16 08:15:00
```

### 2. Export Feature

**Location:** Settings → Data Management → Export to CSV

**Implementation:**
```kotlin
// ExportUseCase
class ExportExpensesToCsvUseCase {
    suspend fun execute(): Result<File> {
        // 1. Get all expenses from database
        // 2. Convert to CSV format
        // 3. Write to file in app's cache directory
        // 4. Return file for sharing
    }
}
```

**UI:**
- Export button in Settings
- Progress indicator during export
- Share sheet to save/share CSV file
- Success/error messages

### 3. Import Feature

**Location:** Settings → Data Management → Import from CSV

**Implementation:**
```kotlin
// ImportUseCase
class ImportExpensesFromCsvUseCase {
    suspend fun execute(csvFile: File): Result<ImportSummary> {
        // 1. Parse CSV file
        // 2. Validate data format
        // 3. Check for duplicates
        // 4. Insert new expenses
        // 5. Return summary (added, skipped, errors)
    }
}
```

**UI:**
- Import button in Settings
- File picker to select CSV
- Preview screen showing what will be imported
- Duplicate handling options (skip/replace/keep both)
- Import summary screen

### 4. Files to Create/Modify

**New Files:**
- `domain/usecase/ExportExpensesToCsvUseCase.kt`
- `domain/usecase/ImportExpensesFromCsvUseCase.kt`
- `util/CsvParser.kt`
- `util/CsvWriter.kt`
- `presentation/screens/DataManagementScreen.kt`
- `presentation/viewmodel/DataManagementViewModel.kt`

**Modified Files:**
- `presentation/screens/SettingsScreen.kt` - Add "Data Management" option
- `presentation/navigation/NavigationRoutes.kt` - Add DATA_MANAGEMENT route

### 5. Dependencies Needed

```kotlin
// build.gradle.kts
dependencies {
    // For CSV parsing (optional, can use manual parsing)
    implementation("com.opencsv:opencsv:5.7.1")
    
    // For file picking
    implementation("androidx.activity:activity-compose:1.8.0")
}
```

---

## Security Considerations

### CSV Export/Import:
- ✅ No sensitive data transmitted over network
- ✅ User controls where files are stored
- ✅ No API keys or credentials needed
- ⚠️ CSV files are unencrypted (user should secure them)

### Google Sheets API:
- ⚠️ Requires OAuth tokens (must be stored securely)
- ⚠️ API keys must not be committed to repository
- ⚠️ Network traffic should use HTTPS
- ⚠️ Implement token refresh logic

---

## User Documentation Needed

### Export Guide:
1. How to export expenses
2. Where exported file is saved
3. How to upload to Google Sheets
4. CSV format explanation

### Import Guide:
1. How to download from Google Sheets as CSV
2. How to import into app
3. Duplicate handling explanation
4. Troubleshooting common errors

---

## Testing Requirements

### Export Testing:
- ✅ Export with no expenses
- ✅ Export with 1 expense
- ✅ Export with 1000+ expenses
- ✅ Export with special characters in description
- ✅ Export with all categories
- ✅ Verify CSV format is correct
- ✅ Verify dates are formatted correctly

### Import Testing:
- ✅ Import valid CSV
- ✅ Import empty CSV
- ✅ Import CSV with invalid data
- ✅ Import CSV with missing columns
- ✅ Import CSV with duplicates
- ✅ Import CSV with unknown categories
- ✅ Import very large CSV (10,000+ rows)

---

## Recommendation

**Start with Phase 1 (CSV Export/Import)** because:

1. **Quick to implement** - Can be done in 1-2 days
2. **No external dependencies** - No API setup needed
3. **Works offline** - No internet required
4. **User control** - User decides when/where to backup
5. **Flexible** - Works with any spreadsheet app
6. **Foundation** - Can build on this for future phases

**Next Steps:**
1. Create a new spec for CSV Export/Import feature
2. Design the UI mockups
3. Implement export functionality
4. Implement import functionality
5. Add to Settings screen
6. Test thoroughly
7. Document for users

Would you like me to create a detailed spec for the CSV Export/Import feature?

---

## Alternative: Quick Export via Share

**Even Simpler Option:**
Add a "Share as CSV" button that:
1. Generates CSV in memory
2. Opens Android share sheet
3. User can save to Drive, email, etc.

This requires **zero file management** and can be implemented in a few hours!

```kotlin
// In ExpenseListScreen
IconButton(onClick = { 
    viewModel.exportAndShare() 
}) {
    Icon(Icons.Default.Share, "Export & Share")
}
```

This gives users immediate backup capability while you decide on the full sync strategy.
