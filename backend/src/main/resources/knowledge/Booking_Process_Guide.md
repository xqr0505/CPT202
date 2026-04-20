## Booking Process Guide

This guide walks new customers through the complete ExpertLink booking journey, from finding a specialist to managing an existing booking.

It is written to match the current product behavior:
- Booking requests are created with **Pending** status and require specialist approval to become **Confirmed**.
- A time slot must be **Available** to book.
- Cancel/reschedule uses a **Quote -> Confirm** flow and can be blocked when too close to the start time.

## Key Terms (Read First)

- **Specialist**: The professional you consult with on ExpertLink.
- **Category**: A specialist domain (for example, Cardiology).
- **Time Slot**: A published appointment window on a specific date (for example, `2026-04-25 14:00-14:30`).
- **Time Slot Status**:
  - **Available**: You can select and book it.
  - **Booked**: Someone already reserved it.
  - **Unavailable**: Not selectable for booking.
- **Booking Status**:
  - **Pending**: You submitted a booking request; the specialist has not approved it yet.
  - **Confirmed**: The specialist approved the booking.
  - **Cancelled**: The booking was cancelled by customer or specialist.
  - **Completed**: The consultation finished.
- **Cancellation/Reschedule Quote**: A preview of refund, penalty, and payable amount shown before you confirm a cancellation or reschedule.
- **Upcoming vs History**:
  - **Upcoming**: Future bookings.
  - **History**: Past records (including cancelled/completed items).

---

### 1. Finding a Specialist

#### 1.1 Using the Search Bar

1. Sign in to your customer account.
2. Open the customer specialist search page.
3. In **Expert name**, type part of a specialist name if you already have one in mind.
4. In **Category**, choose a category to narrow results.
5. In **Available on**, pick a date if you only want specialists who are available on that date.
6. In **Sort by**, choose one option:
   - Recommended
   - Level: High to Low
   - Fee: Low to High
   - Fee: High to Low
7. Click **Search**.
8. Review the result cards and move through pages if needed.
9. Use **Reset** to clear filters and start over.

Questions & Answers

**Q: How do I find a Cardiology specialist?**  
A: Open **Category**, select **Cardiology**, then click **Search**.

**Q: Why did I get no results?**  
A: Your filter combination may be too strict (for example, specific date + category + name). Clear one or more filters and search again.

**Q: I only care about a specific date. How do I find who is available that day?**  
A: Set **Available on** to your target date, leave other filters open, and click **Search**.

**Q: What does “Recommended” sorting mean?**  
A: It is the platform’s default sorting option. If you want a rule you can control, use **Fee** or **Level** sorting.

**Q: I opened a profile and came back, but I lost my filters. How do I avoid that?**  
A: Prefer using the page’s **Back to results** action. The search page keeps filter state in the URL, so returning to that URL preserves your filters.

Troubleshooting

- **Problem: Search returns nothing.**  
  Fix: Remove the date filter first, then retry.
- **Problem: You cannot find a specialist you viewed before.**  
  Fix: Click **Reset**, then search by name only.
- **Problem: Category list looks empty.**  
  Fix: Refresh the page and retry after your network recovers.
- **Problem: The date filter shows the wrong day.**  
  Fix: Double-check your device time zone and system date.

Real-world scenarios

- **Scenario: “I want the cheapest option this week.”**  
  Set **Available on** to your preferred day, set **Sort by** to **Fee: Low to High**, then open a few profiles to compare category match and bio clarity before booking.
- **Scenario: “I want the most senior specialist, price is secondary.”**  
  Choose a category first, set **Sort by** to **Level: High to Low**, then confirm each specialist is **Active** on their profile.
- **Scenario: “I don’t know which category fits my issue.”**  
  Start with a keyword search (name field can also help discover specialists), open 2-3 relevant profiles, and pick the best match by bio and category label.

---

### 2. Viewing Specialist Profiles

#### 2.1 Reading the Bio

1. Click **View** (or open detail) on a specialist card.
2. On the detail page, check:
   - Name and category
   - Specialist level
   - Current status (Active/Inactive)
   - Consultation fee
   - Bio and contact fields (if provided)
3. Confirm the specialist is **Active** before continuing.

What to look for in a specialist bio

- Scope of expertise relevant to your issue.
- Clear description of consultation focus.
- Whether their communication style matches your needs.

Understanding qualifications and certifications

- In the current flow, detailed qualification/review modules are not the primary decision UI.
- Use **category**, **level**, **fee**, and **bio clarity** as your main decision signals.

Questions & Answers

**Q: What if a specialist does not have any reviews?**  
A: Reviews are not the main decision component in the current flow. Focus on category match, profile bio, specialist level, status, and fee.

**Q: The specialist shows “Inactive”. Can I still book and wait for them to accept?**  
A: No. Inactive specialists cannot accept new bookings.

**Q: The profile shows “Unassigned category”. Is that okay?**  
A: You can still read the bio and decide, but if category match is important to you, choose someone with a clear category label.

**Q: Why do I see email/phone as “Not disclosed”?**  
A: Some specialists do not publish direct contact details. Booking and status tracking happen inside ExpertLink.

Troubleshooting

- **Problem: The profile page says the specialist could not be found.**  
  Fix: Go back to search and open another profile (the specialist may have been removed or the link is invalid).
- **Problem: “Book now” is disabled.**  
  Fix: The specialist is likely inactive. Choose another specialist.

Real-world scenarios

- **Scenario: “Two specialists look similar. How do I decide?”**  
  Compare: category match, **Active** status, fee, and whether the bio describes your exact topic. If both work, choose the one with better availability on your preferred date.
- **Scenario: “I found the right specialist but I’m not ready to book.”**  
  Note the specialist name and fee, then return later and search by name to find them again quickly.

#### 2.2 Checking Availability

1. Click **Book now** from the specialist detail page.
2. On the booking page, locate **Available time slots**.
3. Choose a date from the date picker.
4. Review each slot’s status label:
   - Available
   - Booked
   - Unavailable
5. Select only an **Available** slot.

Time zone note

- The interface shows date and time based on your device/browser context. If you travel or switch devices, confirm the date carefully before submitting.

Questions & Answers

**Q: What if there are no available times?**  
A: Try a different date or choose another specialist.

**Q: Why do I see slots marked “Booked” even if I didn’t book them?**  
A: Time slots are shared inventory. Another customer may have reserved the slot already.

**Q: What does “Unavailable” mean?**  
A: It is not selectable. Pick another slot that is **Available**.

Troubleshooting

- **Problem: A slot looks selectable but warns it is booked.**  
  Fix: Availability can change quickly. Pick another available slot and continue.
- **Problem: No slots exist on multiple dates.**  
  Fix: Choose another specialist or remove the “Available on” filter in search to broaden options.

Real-world scenarios

- **Scenario: “I need an appointment tomorrow.”**  
  Keep the date fixed and try multiple specialists in the same category until you find an **Available** slot.
- **Scenario: “I can only do evenings.”**  
  Try different dates and look for later start times. If none exist, consider another specialist.

---

### 3. Booking an Appointment

#### 3.1 Selecting a Time Slot

1. On the booking page, pick a date.
2. Click one **Available** slot.
3. Confirm the selected time appears in the booking summary panel.

Questions & Answers

**Q: What does “Pending” status mean?**  
A: Your request was submitted successfully, but the specialist must approve it before it becomes **Confirmed**.

**Q: Is the slot reserved immediately after I submit?**  
A: Yes. Once the booking is created, the selected slot is marked as booked and cannot be selected by others.

**Q: Can I select a slot that says “Booked” or “Unavailable”?**  
A: No. Only **Available** slots can be selected.

Troubleshooting

- **Problem: Slot selection disappears after changing the date.**  
  Fix: This is expected. Select a new available slot for the new date.
- **Problem: You selected a slot but the summary still says “Please choose a slot”.**  
  Fix: Click the slot again, then wait a moment for the UI to update. If it persists, refresh the page and retry.

Real-world scenarios

- **Scenario: “I clicked a slot, but it turned booked while I was filling the form.”**  
  This can happen. Select a different available slot and submit again.

#### 3.2 Providing Information

1. In **Topic**, select one available consultation topic.
2. In **Notes**, optionally add context for the specialist.
3. Keep notes clear and short:
   - What’s happening
   - How long it has been happening
   - What you want to achieve in the consultation
4. Avoid unsupported characters if a validation warning appears.

What to include in notes

- Main issue or symptom timeline.
- Your goal for the consultation.
- Any constraints (for example, “I need guidance before next Monday”).

Questions & Answers

**Q: Do I have to write notes to book?**  
A: No. Notes are optional, but **Topic** is required.

**Q: What kind of information should I include in notes?**  
A: Add a brief, practical summary: what is happening, how long it has lasted, and what outcome you want from the session.

**Q: Why does it say “Notes contain unsupported characters”?**  
A: Notes accept common letters, numbers, punctuation, spaces, and line breaks. Remove unusual symbols and try again.

**Q: Is there a maximum length for notes?**  
A: Keep notes short. Very long notes may be trimmed by the system, so put the most important information first.

Troubleshooting

- **Problem: Cannot submit because of notes format warning.**  
  Fix: Remove unsupported characters and submit again.
- **Problem: Topic selection is empty.**  
  Fix: Wait for the topic list to load, then retry. If it stays empty, refresh the page.
- **Problem: You selected a topic, but it clears unexpectedly.**  
  Fix: Refresh and re-select the topic (your browser session may have been interrupted).

Real-world scenarios (examples of good notes)

- **Scenario: First consultation, unclear cause**  
  “I have had intermittent chest discomfort for 2 weeks. No emergency symptoms. I want help understanding likely causes and what tests to consider. I’m available for follow-up next week.”
- **Scenario: Clear second-opinion request**  
  “I want a second opinion on a lab report. My goal is to understand which values matter and what lifestyle changes are recommended.”
- **Scenario: Reschedule context**  
  “Same topic as before. I can’t attend the original time. Please keep the key focus on interpreting my recent test results.”

#### 3.3 Payment and Confirmation

1. Review the booking summary (specialist, time slot, fee, topic, notes).
2. Click **Confirm booking**.
3. If successful, the booking is created with **Pending** status.
4. You are redirected to **My Bookings**.

Important truth about payment in the current flow

- The consultation fee is displayed during booking.
- There is no separate online checkout step on the booking page.
- Financial amounts (refund/penalty/payable) are presented in the cancel/reschedule quote-confirm flow.

Questions & Answers

**Q: I clicked Confirm. Where do I see the result?**  
A: You’ll be redirected to **My Bookings**, and you can confirm the new entry status is **Pending**.

**Q: What if I clicked Confirm multiple times?**  
A: The system prevents duplicate processing. Wait for the request to finish, then check **My Bookings**.

Troubleshooting

- **Problem: Error says time slot already booked.**  
  Fix: The slot was taken just before your submission. Your topic/notes were kept; choose another slot and submit again.
- **Problem: Duplicate request warning appears.**  
  Fix: Wait for the current request to finish; avoid rapid repeated clicks.
- **Problem: You get a generic “Failed to create booking.”**  
  Fix: Refresh and retry once. If it persists, contact support with the time slot and specialist details.

Real-world scenarios

- **Scenario: “I booked successfully but picked the wrong slot.”**  
  If the booking start time is more than 2 hours away, use **Reschedule** from **My Bookings** rather than creating multiple bookings.

---

### 4. Managing Your Bookings

#### 4.1 Viewing Upcoming Appointments

1. Open **My Bookings** from the customer area.
2. Use tabs:
   - **Upcoming** for future bookings.
   - **History** for past/cancelled/completed records.
3. Use status filters (All, Pending, Confirmed, Cancelled, Completed).
4. Click **View Details** to inspect one booking.

Questions & Answers

**Q: How do I reschedule an appointment?**  
A: In **My Bookings -> Upcoming**, click **Reschedule** on a booking that is still eligible.

**Q: Where do I check whether my booking was approved?**  
A: In **My Bookings**, look at the **Status** column. It changes from **Pending** to **Confirmed** after specialist approval.

**Q: What’s the difference between Upcoming and History?**  
A: Upcoming is for future bookings. History contains past records (including completed or cancelled items).

Troubleshooting

- **Problem: You cannot see a booking you just created.**  
  Fix: Stay on **Upcoming**, set status to **All**, and refresh.
- **Problem: You can’t find a booking by scrolling.**  
  Fix: Use status filters to narrow down (for example, filter by **Pending**).

Real-world scenarios

- **Scenario: “I want to book the same specialist again.”**  
  In **History**, use **Book Again** to jump back to that specialist’s profile, then select a new slot.

#### 4.2 Cancelling or Rescheduling

Step-by-step cancellation

1. In **Upcoming**, click **Cancel** on an eligible booking.
2. Read the cancellation quote (refund + penalty + policy message).
3. Click **Confirm Cancel** if allowed.
4. Booking status changes to **Cancelled**.

Step-by-step reschedule

1. In **Upcoming**, click **Reschedule** on an eligible booking.
2. Pick a new date.
3. Select a new available slot.
4. Review the reschedule quote:
   - Price difference
   - Penalty amount
   - Refund amount
   - Payable amount
5. Click **Confirm Reschedule**.
6. After reschedule, booking returns to **Pending** for specialist approval.

Current policy behavior (important)

- Only **Pending** and **Confirmed** bookings can be changed.
- If booking start time is within **2 hours**, cancel/reschedule is blocked.
- More than **24 hours** before start: full-refund policy window.
- Between **2 and 24 hours** before start: penalty policy window.

Questions & Answers

**Q: What are the cancellation fees?**  
A: Fees are calculated dynamically by policy and shown in the cancellation quote before you confirm.

**Q: Why do I need to see a quote before cancelling/rescheduling?**  
A: It is a confirmation step so you can see the calculated penalty/refund/payable amount before you commit.

**Q: I rescheduled successfully. Why did the status go back to Pending?**  
A: Rescheduling creates a new request that must be approved again by the specialist.

**Q: Can I reschedule to a different specialist?**  
A: No. Reschedule keeps the same specialist; you can only choose a different time slot for that specialist.

**Q: Why are Cancel/Reschedule buttons disabled?**  
A: Common reasons: the booking is not **Pending/Confirmed**, or the start time is within 2 hours.

Troubleshooting

- **Problem: Cancel/Reschedule button is disabled.**  
  Fix: Check status and start time. If you are within 2 hours, changes are blocked by policy.
- **Problem: Cancellation quote shows “not allowed”.**  
  Fix: Read the message; it usually indicates “too close to start” or invalid status.
- **Problem: Reschedule confirm fails.**  
  Fix: The new slot may have been taken. Select another available slot and retry.

Real-world scenarios

- **Scenario: “I booked for tomorrow but need to move it.”**  
  Try **Reschedule** first. If blocked because it’s within 2 hours, keep the booking or contact support.
- **Scenario: “I want to cancel but avoid penalties.”**  
  Cancel earlier than 24 hours before the start time to stay in the full-refund window.
- **Scenario: “I rescheduled, but now I’m waiting again.”**  
  That’s expected: reschedule returns the booking to **Pending** and requires specialist approval again.

---

### 5. Troubleshooting Common Issues

#### 5.1 Contacting Support

How to contact support:
- Email: `2906326615@qq.com`

What to include for faster help:
1. Your account email.
2. Booking ID (if available).
3. Specialist name and selected date/time.
4. Exact action you were taking (search, book, cancel, reschedule).
5. Error message text (copy full message).
6. Screenshot or short screen recording if possible.

Common issue checklist before contacting support:
- Refresh the page and retry once.
- Check if your selected slot changed status.
- Confirm the specialist is active.
- Confirm the booking is more than 2 hours away for changes.

Additional common questions

**Q: The system says “Time slot already booked”. Why does this happen?**  
A: Another booking reserved the slot first, or the slot status changed between selection and submission. Choose another available slot.

**Q: I can’t cancel/reschedule, but I believe I should be able to.**  
A: Confirm the start time is more than 2 hours away and the booking status is **Pending** or **Confirmed**. If both are true, contact support with booking ID and screenshot of the disabled action.

---

Real-world quick scenarios

- **Scenario A: First-time customer with urgent but non-emergency consultation need**  
  Search by category, set the nearest available date, sort by fee, pick an active specialist, submit concise notes, then track **Pending -> Confirmed** in My Bookings.
- **Scenario B: Customer needs to move tomorrow’s appointment**  
  Open Upcoming, try Reschedule, review the quote, pick a new slot, confirm, then wait for specialist approval again because status returns to Pending.
- **Scenario C: Customer clicked Confirm multiple times**  
  The system blocks duplicate submission attempts. Wait for processing and check My Bookings for the created booking entry.
- **Scenario D: Specialist rejected the request (booking becomes Cancelled)**  
  If a specialist rejects, the booking becomes **Cancelled** and the slot is released. Book another slot or choose another specialist.
- **Scenario E: Customer picked the wrong topic**  
  If the booking is still more than 2 hours away, use **Reschedule** to pick a new slot and re-submit with the correct topic (status returns to Pending).
