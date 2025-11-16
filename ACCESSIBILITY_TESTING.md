# Accessibility Testing Guide

## Quick Start Testing with TalkBack

### Enable TalkBack
1. Go to **Settings → Accessibility → TalkBack**
2. Toggle TalkBack on
3. Or use quick shortcut: Press and hold **Volume Up + Volume Down** for 3 seconds

### Basic TalkBack Gestures
- **Swipe Right**: Move to next element
- **Swipe Left**: Move to previous element
- **Double Tap**: Activate focused element
- **Swipe Down then Right**: Read from current position
- **Swipe Up then Down**: Read entire screen

## Test Scenarios

### 1. Expense List Screen

**Test Steps**:
1. Launch app with TalkBack enabled
2. Swipe right through elements
3. Verify announcements:
   - "Expense Tracker" (heading)
   - "Filter expenses button"
   - "View spending summary button"
   - Each expense item with full details
   - "Add new expense button"

**Expected Behavior**:
- All buttons are announced with clear labels
- Expense items announce: amount, category, date, description
- Swipe gestures on items announce delete action
- Filter chips announce active filters

### 2. Add/Edit Expense Screen

**Test Steps**:
1. Navigate to add expense screen
2. Swipe through form fields
3. Try entering invalid data
4. Verify error announcements

**Expected Behavior**:
- "Add Expense" or "Edit Expense" heading announced
- Amount field announces validation state
- Category selector announces selected category
- Date selector announces selected date
- Save button announces disabled state when form invalid
- Validation errors are announced

### 3. Filter Screen

**Test Steps**:
1. Navigate to filter screen
2. Swipe through date selectors
3. Navigate through category checkboxes
4. Verify selection states

**Expected Behavior**:
- "Filter Expenses" heading announced
- Date selectors announce selected dates
- Category checkboxes announce selection state
- Active filter count is announced
- Apply and Clear buttons are clearly labeled

### 4. Summary Screen

**Test Steps**:
1. Navigate to summary screen
2. Swipe through period selector
3. Navigate through category breakdown

**Expected Behavior**:
- "Spending Summary" heading announced
- Total spending amount announced with period
- Period chips announce selection state
- Category items announce name, amount, and percentage
- Pie chart purpose is announced

## Touch Target Verification

### Visual Inspection
1. Enable **Developer Options → Show Layout Bounds**
2. Verify all interactive elements are at least 48dp × 48dp
3. Check buttons, cards, chips, and list items

### Elements to Verify
- ✓ All buttons (Save, Cancel, Apply, Clear, etc.)
- ✓ FAB (Add expense button)
- ✓ Icon buttons (Back, Filter, etc.)
- ✓ List items (Expense cards)
- ✓ Filter chips
- ✓ Category checkboxes
- ✓ Date selectors

## Keyboard Navigation Testing

### Test Steps
1. Connect external keyboard to device
2. Use **Tab** to navigate between elements
3. Use **Enter/Space** to activate buttons
4. Verify focus indicators are visible

**Expected Behavior**:
- Tab order follows logical reading flow
- All interactive elements are reachable
- Focus indicators are clearly visible
- Enter/Space activates focused elements

## Screen Reader Content Verification

### Checklist for Each Screen

**ExpenseListScreen**:
- [ ] Screen title is announced as heading
- [ ] All buttons have descriptive labels
- [ ] Expense items provide complete information
- [ ] Empty state is properly announced
- [ ] Loading state is announced
- [ ] Error messages are accessible

**AddEditExpenseScreen**:
- [ ] Form fields have clear labels
- [ ] Validation errors are announced
- [ ] Required fields are indicated
- [ ] Character counts are announced
- [ ] Button states are communicated

**FilterScreen**:
- [ ] Section headings are announced
- [ ] Date selectors provide instructions
- [ ] Category selection states are clear
- [ ] Filter count is announced
- [ ] Action buttons are clearly labeled

**SummaryScreen**:
- [ ] Total spending is prominently announced
- [ ] Period selector states are clear
- [ ] Category breakdown is accessible
- [ ] Chart purpose is described
- [ ] All amounts are properly formatted

## Common Issues to Check

### Content Description Issues
- Missing content descriptions on icons
- Generic descriptions like "button" or "icon"
- Descriptions that don't match visual content
- Overly verbose descriptions

### Touch Target Issues
- Interactive elements smaller than 48dp
- Insufficient spacing between touch targets
- Overlapping touch areas

### Navigation Issues
- Illogical focus order
- Trapped focus (can't navigate away)
- Missing focusable elements
- Decorative elements in focus order

### State Announcement Issues
- Loading states not announced
- Error states not communicated
- Success confirmations missing
- Disabled state not indicated

## Automated Testing

### Using Accessibility Scanner

1. Install **Accessibility Scanner** from Play Store
2. Enable the scanner
3. Navigate through each screen
4. Review scanner suggestions
5. Fix any issues found

### Using Android Studio

1. Open **Analyze → Run Inspection by Name**
2. Type "Accessibility"
3. Run accessibility inspections
4. Review and fix issues

## Regression Testing

After any UI changes, re-test:
1. All interactive elements have content descriptions
2. Touch targets meet minimum size
3. Screen reader navigation is logical
4. Form validation is accessible
5. State changes are announced

## Success Criteria

The app passes accessibility testing when:
- ✓ All screens are navigable with TalkBack
- ✓ All interactive elements are properly labeled
- ✓ Touch targets meet 48dp minimum
- ✓ Form validation is accessible
- ✓ Loading and error states are announced
- ✓ Navigation order is logical
- ✓ No accessibility scanner warnings
- ✓ Users can complete all tasks with screen reader

## Resources

- [Android Accessibility Testing](https://developer.android.com/guide/topics/ui/accessibility/testing)
- [TalkBack Gestures](https://support.google.com/accessibility/android/answer/6151827)
- [Accessibility Scanner](https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor)
