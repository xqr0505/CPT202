## Booking Process Guide (RAG-Optimized FAQ Edition)

This expanded guide is a faithful rewrite of the current ExpertLink customer booking flow.
It does not introduce new business rules. It only restructures and expands the same logic into highly retrievable, atomic question-answer units for better context recall and answer faithfulness.

### Scope and Product Truths (Authoritative Baseline)

- A booking request is created as **Pending** and requires specialist approval to become **Confirmed**.
- A slot must be **Available** to be booked.
- Slot statuses are: **Available**, **Booked**, **Unavailable**.
- Booking statuses are: **Pending**, **Confirmed**, **Cancelled**, **Completed**.
- Cancel and reschedule both use a **Quote -> Confirm** flow.
- Cancel/reschedule eligibility can be blocked when too close to start time.
- Only **Pending** and **Confirmed** bookings can be changed.
- If start time is within **2 hours**, cancel/reschedule is blocked.
- More than **24 hours** before start is the full-refund policy window.
- Between **2 and 24 hours** before start is the penalty policy window.
- Rescheduling keeps the same specialist and sends the booking back to **Pending** for re-approval.
- The booking page shows consultation fee, but there is no separate online checkout step there.
- Financial calculations are shown inside cancel/reschedule quote details.

### Key Terms (Atomic Definitions)

**Q: What is a specialist on ExpertLink?**  
**A:** A specialist is the professional you consult with on the platform.

**Q: What is a category?**  
**A:** A category is the specialist domain, such as Cardiology.

**Q: What is a time slot?**  
**A:** A time slot is a published appointment window on a specific date, for example `2026-04-25 14:00-14:30`.

**Q: What does slot status Available mean?**  
**A:** **Available** means you can select, choose, reserve, and submit a booking request for that slot.

**Q: What does slot status Booked mean?**  
**A:** **Booked** means another customer already reserved that shared slot inventory.

**Q: What does slot status Unavailable mean?**  
**A:** **Unavailable** means the slot is not selectable and cannot be used for booking.

**Q: What does booking status Pending mean?**  
**A:** **Pending** means your request was submitted successfully and is waiting for specialist approval.

**Q: What does booking status Confirmed mean?**  
**A:** **Confirmed** means the specialist approved the booking request.

**Q: What does booking status Cancelled mean?**  
**A:** **Cancelled** means the booking was cancelled by customer or specialist.

**Q: What does booking status Completed mean?**  
**A:** **Completed** means the consultation already finished.

**Q: What is a cancellation or reschedule quote?**  
**A:** It is a preview that shows refund, penalty, and payable amounts before you confirm cancellation or rescheduling.

**Q: What is Upcoming vs History in My Bookings?**  
**A:** **Upcoming** contains future bookings. **History** contains past records, including cancelled and completed bookings.

### 1) Search and Discover Specialists

**Q: How do I find, locate, discover, or search for a specialist?**  
**A:** Sign in, open the customer specialist search page, set filters (name, category, date, sort), then click **Search**.

**Q: How do I search by expert name if I only remember part of the name?**  
**A:** Type any partial name in **Expert name** and click **Search** to narrow, locate, and retrieve matching profiles.

**Q: How do I find a specialist in Cardiology?**  
**A:** Choose **Cardiology** in **Category**, then click **Search**.

**Q: How do I find who is available on a specific day?**  
**A:** Set **Available on** to your target date, then click **Search**.

**Q: How do I sort results from cheapest to most expensive?**  
**A:** Use **Sort by -> Fee: Low to High**.

**Q: How do I sort by highest specialist level?**  
**A:** Use **Sort by -> Level: High to Low**.

**Q: What does Recommended sorting mean?**  
**A:** It is the platform default sorting option. If you want explicit control, use fee or level sorting.

**Q: Why am I getting no results after searching?**  
**A:** Your filters may be too strict in combination (name + category + date). Remove one filter, broaden conditions, and search again.

**Q: I previously saw a specialist but now cannot find them. What should I do?**  
**A:** Click **Reset** and search again by name only to remove restrictive filters.

**Q: What does Reset do in search?**  
**A:** **Reset** clears all filter conditions so you can restart search from a clean state.

**Q: Can I keep my filter state when returning from a profile page?**  
**A:** Yes. Prefer using **Back to results**. The search page keeps state in URL, so returning to that URL preserves filters.

**Q: If I only care about this weekend, what is the fastest search method?**  
**A:** Use **Available on** for the exact date, keep other filters broad, then compare returned specialists.

**Q: If I care about budget first, what is the best search strategy?**  
**A:** Search by category and date, then use **Fee: Low to High** to rank options by lower fee first.

**Q: If I care about seniority first, what is the best search strategy?**  
**A:** Filter by category and sort using **Level: High to Low**, then confirm specialist status is **Active** on profile.

**Q: If I am unsure about category, how can I still discover options?**  
**A:** Start broad, run a lighter search, open several profiles, and choose based on bio clarity, category label, level, and fee.

**Troubleshooting: Search**

**Q: Search returns zero cards every time. How do I recover quickly?**  
**A:** First remove the date filter, then run search again with fewer constraints.

**Q: Category list appears empty. Is that normal?**  
**A:** Usually this is temporary loading or network delay. Refresh and retry after connection stabilizes.

**Q: Date filter seems off by one day. What should I verify?**  
**A:** Check your device time zone and system date/time settings.

**Q: I clicked Search but nothing changed. What can I do?**  
**A:** Retry once after refresh, and confirm at least one visible filter value changed before searching again.

**Pro-Tip: Search**

**Q: What is a reliable, low-friction search sequence for faster decisions?**  
**A:** Use this order: category -> date -> sort by fee or level -> open top 2-3 profiles -> verify **Active** -> book.

### 2) Specialist Profiles and Decision-Making

**Q: What should I check first on a specialist profile?**  
**A:** Check name, category, level, status (Active/Inactive), fee, and bio.

**Q: Why is Active status important before I book?**  
**A:** Only active specialists can proceed normally in the booking flow; inactive specialists cannot accept new bookings.

**Q: Can I book an Inactive specialist and wait for approval?**  
**A:** No. Inactive specialists cannot accept new bookings.

**Q: How should I evaluate profile quality when reviews are not primary in this flow?**  
**A:** Focus on category match, specialist level, fee, status, and whether the bio clearly describes relevant consultation scope.

**Q: Is it okay if profile shows Unassigned category?**  
**A:** You can still evaluate and decide by bio and fee, but if strict category match matters, choose a profile with a clear category label.

**Q: Why are phone or email fields not disclosed for some specialists?**  
**A:** Some specialists do not publish direct contact details; booking and status tracking happen inside ExpertLink.

**Q: How do I compare two similar specialists quickly?**  
**A:** Compare category relevance, **Active** status, fee, level, and availability on your preferred date.

**Q: I found a good specialist but cannot book now. How do I return later?**  
**A:** Save the specialist name and fee, then search by name later for quick rediscovery.

**Troubleshooting: Profile**

**Q: Profile says specialist not found. What does that mean?**  
**A:** The link may be invalid or the specialist may no longer be available. Return to search and open another result.

**Q: Book now is disabled on profile. Why?**  
**A:** Common reason is specialist is inactive. Choose another specialist.

**Pro-Tip: Profile**

**Q: What profile signal is most practical for booking confidence?**  
**A:** Prioritize clear bio scope plus active status; then use fee and level for final tradeoff.

### 3) Availability and Time Slot Selection

**Q: How do I check appointment availability after opening a profile?**  
**A:** Click **Book now**, choose a date, then inspect slot labels under available time slots.

**Q: Which slot status can I actually click and book?**  
**A:** Only **Available** slots are selectable for booking.

**Q: Why do I see Booked slots even if I never selected them?**  
**A:** Slots are shared inventory; other customers can reserve them before you.

**Q: What should I do if all slots on a date are unavailable?**  
**A:** Try another date for the same specialist or choose a different specialist.

**Q: Can availability change while I am viewing the page?**  
**A:** Yes. Inventory is dynamic, so a slot may change from available to booked.

**Q: Why did a clickable slot fail with a booked warning?**  
**A:** The slot changed state during your action; pick another currently available slot.

**Q: I need a near-term appointment. What is the practical strategy?**  
**A:** Keep date fixed and compare multiple specialists in the same category until you find an available slot.

**Q: I only want evening times. How should I search?**  
**A:** Check several dates and specialists until later start times appear as **Available**.

**Q: Does the displayed schedule depend on device context?**  
**A:** Yes. Date/time display follows device or browser context, so confirm date carefully if switching devices or traveling.

**Troubleshooting: Availability**

**Q: No slots exist for many dates. Is booking impossible?**  
**A:** For that specialist and date range, there may be no inventory. Broaden date, remove strict filters, or choose another specialist.

**Q: Selected slot disappears after date change. Is this a bug?**  
**A:** This behavior is expected. Slot selection is date-specific and resets when date changes.

**Pro-Tip: Availability**

**Q: How do I reduce slot-loss risk before submission?**  
**A:** Select slot first, then immediately finalize topic/notes and submit without long delays.

### 4) Booking Creation (Slot, Topic, Notes, Confirm)

**Q: What are the required steps to create a booking request?**  
**A:** Choose date -> select one **Available** slot -> select **Topic** -> optionally add **Notes** -> click **Confirm booking**.

**Q: Is Topic required? Are Notes required?**  
**A:** **Topic** is required. **Notes** are optional.

**Q: What should I write in notes for better consultation context?**  
**A:** Write a short summary: what is happening, how long, and desired consultation outcome.

**Q: What if I type unusual symbols and get a validation warning?**  
**A:** Remove unsupported characters and use common letters, numbers, punctuation, spaces, and line breaks.

**Q: Is there a notes length limit?**  
**A:** Keep notes short and put key points first; very long notes may be trimmed.

**Q: I selected a slot but summary still says choose a slot. What should I do?**  
**A:** Click the slot again and wait for UI update. If still unresolved, refresh and reselect.

**Q: Topic list appears empty. How do I proceed?**  
**A:** Wait for data loading; if still empty, refresh and retry.

**Q: I clicked Confirm booking. Where do I verify success?**  
**A:** You are redirected to **My Bookings** where the new item appears with **Pending** status.

**Q: What exactly does Pending mean right after booking?**  
**A:** The request exists and slot is reserved, but specialist approval is still required.

**Q: Is the slot reserved once I submit booking?**  
**A:** Yes. After booking creation, the slot becomes booked and is no longer selectable by others.

**Q: What if time slot already booked error appears during submit?**  
**A:** Another user reserved it first. Your topic/notes are retained; choose another available slot and submit again.

**Q: I clicked confirm several times. Will this duplicate my booking?**  
**A:** System duplicate protection prevents repeated processing; wait for completion and verify in My Bookings.

**Q: Why does booking fail with generic creation error?**  
**A:** Refresh and retry once. If it persists, contact support with specialist and slot details.

**Q: Where is payment checkout during booking?**  
**A:** There is no separate online checkout step on the booking page in the current flow.

**Q: If there is no checkout step, where do financial numbers appear?**  
**A:** Financial details (refund, penalty, payable) appear in cancel/reschedule quote-confirm flow.

**Troubleshooting: Booking Submission**

**Q: My selected topic keeps clearing. Is there a workaround?**  
**A:** Refresh the page, reselect topic, then submit in one continuous flow.

**Q: My notes triggered format errors repeatedly. What format is safest?**  
**A:** Use concise plain text with standard characters and short sentences.

**Q: I booked the wrong slot by mistake. What is the best correction path?**  
**A:** If start time is more than 2 hours away, use **Reschedule** in My Bookings instead of making duplicate bookings.

**Pro-Tip: Booking**

**Q: What is the fastest way to increase approval clarity for specialists?**  
**A:** Use a specific topic and a short notes structure: issue timeline + consultation goal + timing context.

### 5) My Bookings: Tracking and Navigation

**Q: Where do I track newly created booking requests?**  
**A:** Open **My Bookings** in the customer area.

**Q: How do I switch between future and past records?**  
**A:** Use **Upcoming** for future items and **History** for past/cancelled/completed records.

**Q: How do I filter by booking status?**  
**A:** Use status filters: **All**, **Pending**, **Confirmed**, **Cancelled**, **Completed**.

**Q: How do I verify specialist approval status?**  
**A:** In My Bookings, check status transition from **Pending** to **Confirmed**.

**Q: I cannot find my new booking. Where should I look first?**  
**A:** Stay in **Upcoming**, set filter to **All**, then refresh.

**Q: I need full details for one booking record. What should I open?**  
**A:** Click **View Details** on that booking.

**Q: How do I quickly book same specialist again from previous records?**  
**A:** In **History**, use **Book Again** to return to that specialist profile and choose a new slot.

**Troubleshooting: My Bookings**

**Q: I scrolled but still cannot locate one booking. Any precise method?**  
**A:** Use status filters to narrow records, then open details for verification.

**Q: Booking appears in History unexpectedly. Why?**  
**A:** History contains past outcomes including cancelled/completed records.

**Pro-Tip: My Bookings**

**Q: What is the cleanest monitoring routine after submission?**  
**A:** Check Upcoming with filter **Pending** first, then switch to **Confirmed** to monitor approvals efficiently.

### 6) Cancellation and Rescheduling (Policy-Faithful FAQ)

**Q: How do I cancel an eligible booking?**  
**A:** In **Upcoming**, click **Cancel**, review cancellation quote, then click **Confirm Cancel** if allowed.

**Q: How do I reschedule an eligible booking?**  
**A:** In **Upcoming**, click **Reschedule**, pick new date and available slot, review quote, then click **Confirm Reschedule**.

**Q: What appears in the reschedule quote?**  
**A:** Price difference, penalty amount, refund amount, and payable amount.

**Q: Which statuses can be cancelled or rescheduled?**  
**A:** Only **Pending** and **Confirmed** bookings are change-eligible.

**Q: Why are Cancel or Reschedule buttons greyed out or disabled?**  
**A:** Usually because status is not Pending/Confirmed, or start time is within 2 hours.

**Q: What is the hard time lock for changes?**  
**A:** If booking start is within **2 hours**, cancel/reschedule is blocked.

**Q: What is the full-refund timing window?**  
**A:** More than **24 hours** before start time.

**Q: What is the penalty timing window?**  
**A:** Between **2 and 24 hours** before start time.

**Q: Are cancellation fees fixed or dynamic?**  
**A:** They are policy-calculated dynamically and shown in quote before confirmation.

**Q: Why is quote mandatory before confirm?**  
**A:** It is a decision checkpoint so you can review financial impact before final action.

**Q: Can I reschedule to another specialist?**  
**A:** No. Reschedule keeps the same specialist and only changes time slot.

**Q: Why did status become Pending after successful reschedule?**  
**A:** Reschedule creates a fresh approval request that must be approved again.

**Q: Reschedule confirm failed suddenly. What happened?**  
**A:** The newly chosen slot may have been taken. Select another available slot and retry.

**Q: Cancellation quote says not allowed. What does it usually mean?**  
**A:** Most often it indicates invalid status or too close to start time.

**Q: Is it better to cancel and rebook, or use reschedule?**  
**A:** If eligible, use **Reschedule** for time changes with the same specialist and quote transparency.

**Q: I need to move appointment by a small amount of time. What is fastest?**  
**A:** Use **Reschedule** instead of cancel + new booking, as long as eligibility rules allow it.

**Q: If I want to avoid penalties, when should I act?**  
**A:** Cancel earlier than 24 hours before start to stay in the full-refund window.

**Q: Can completed bookings be rescheduled?**  
**A:** No. Only Pending and Confirmed bookings can be changed.

**Q: Can cancelled bookings be changed again?**  
**A:** No. Cancelled is not an eligible change status.

**Q: If specialist rejects my request, what is the outcome?**  
**A:** Booking becomes **Cancelled** and the slot is released.

**Troubleshooting: Cancel/Reschedule**

**Q: I believe I am eligible but button still disabled. What should I verify?**  
**A:** Verify both conditions: status is Pending/Confirmed and start time is more than 2 hours away.

**Q: I opened quote but cannot proceed. What can I do?**  
**A:** Re-read policy message in quote, then choose another slot/date if available or keep existing booking.

**Q: I rescheduled and now I am waiting again. Is this expected?**  
**A:** Yes. Pending after reschedule is expected until specialist re-approves.

**Pro-Tip: Cancel/Reschedule**

**Q: How do I reduce friction when changing plans?**  
**A:** Check eligibility first (status + time window), then complete quote review and confirmation in one uninterrupted flow.

### 7) Support and Escalation

**Q: How can I contact support?**  
**A:** Email: `2906326615@qq.com`.

**Q: What details should I include for faster support resolution?**  
**A:** Account email, booking ID, specialist name, selected date/time, action attempted, full error text, and screenshot if possible.

**Q: What should I try before contacting support?**  
**A:** Refresh once, recheck slot status, verify specialist is active, and confirm 2-hour policy eligibility for changes.

**Q: I see “time slot already booked” repeatedly. Is this support-only?**  
**A:** Usually no. It is often inventory contention; choose another available slot first.

**Q: I think cancel/reschedule should be allowed but is blocked. What proof helps support most?**  
**A:** Provide booking ID and screenshot showing disabled action plus visible booking time/status.

### 8) Expanded Implied Questions (30+ Diverse, Atomic, Business-Faithful)

**Q: My booking is Pending for a while. Is this normal?**  
**A:** Yes. Pending means waiting for specialist approval.

**Q: Is Pending the same as confirmed appointment?**  
**A:** No. Pending is submitted but not yet approved.

**Q: Can I book a slot marked Unavailable if it looks close to my preferred time?**  
**A:** No. Only Available slots are selectable.

**Q: Can two customers hold the same available slot at once?**  
**A:** No. Once booking is created, that slot becomes booked.

**Q: Why did my selected slot change while I was entering notes?**  
**A:** Shared inventory can change in real time.

**Q: I forgot to select topic and tried to submit. Why blocked?**  
**A:** Topic is required for booking submission.

**Q: Can I leave notes empty and still book?**  
**A:** Yes. Notes are optional.

**Q: Does fee sorting change specialist eligibility?**  
**A:** No. Sorting changes order, not eligibility.

**Q: Does level sorting guarantee earliest availability?**  
**A:** No. Availability still depends on each specialist’s slots.

**Q: If category is correct but date is strict, can I still get zero results?**  
**A:** Yes. Date restriction can remove all matches.

**Q: Can I reschedule a booking in History tab?**  
**A:** Reschedule actions are performed from eligible upcoming bookings.

**Q: Why is quote shown before final cancel action?**  
**A:** To show refund/penalty/payable before commitment.

**Q: If I only need a small time adjustment, should I create a new booking?**  
**A:** Prefer reschedule if eligible.

**Q: If I reschedule successfully, do I skip specialist approval?**  
**A:** No. It returns to Pending for approval.

**Q: Are cancellation fees visible before I confirm cancel?**  
**A:** Yes. They are shown in quote preview.

**Q: Is there a separate card-payment page while creating booking?**  
**A:** No separate checkout step exists on booking page.

**Q: Where do I see my latest request immediately after confirm?**  
**A:** My Bookings -> Upcoming.

**Q: Can inactive specialists accept new bookings later in the same flow?**  
**A:** Inactive status means cannot accept new bookings now.

**Q: If I cannot find a specialist by filters, what is safest fallback?**  
**A:** Reset filters and search by name or broader category/date.

**Q: Why does my date-specific search seem too narrow?**  
**A:** Combined constraints reduce result set quickly.

**Q: Can I change specialist during reschedule?**  
**A:** No. Reschedule keeps same specialist.

**Q: If cancel button is disabled exactly near start time, is that policy behavior?**  
**A:** Yes. Within 2 hours, changes are blocked.

**Q: What happens if specialist rejects my request?**  
**A:** Booking becomes Cancelled.

**Q: Do I need to call specialist externally to confirm appointment?**  
**A:** Booking lifecycle is tracked in-platform via status.

**Q: If contact fields are hidden, can I still complete booking flow?**  
**A:** Yes. Contact disclosure is optional and not required for booking.

**Q: Can I rely on profile reviews as primary decision input here?**  
**A:** In current flow, use category, level, status, fee, and bio as main signals.

**Q: Why does Book Again help repeat appointments?**  
**A:** It quickly navigates back to previous specialist for a fresh slot selection.

**Q: If I spam-click confirm, can system create many bookings?**  
**A:** Duplicate processing is prevented.

**Q: Why does reschedule sometimes fail at final step despite valid status?**  
**A:** Target slot may be taken before confirmation.

**Q: Can I cancel completed consultation to get quote?**  
**A:** No. Change eligibility is limited to Pending/Confirmed.

**Q: If I switch device, what should I double-check before booking?**  
**A:** Confirm date/time display based on device/browser context.

**Q: Is Upcoming only for confirmed bookings?**  
**A:** No. Upcoming includes future bookings, including pending and confirmed.

**Q: Is History only for completed bookings?**  
**A:** No. History includes completed and cancelled records.

**Q: What is the minimum data to send support for action-level troubleshooting?**  
**A:** Booking ID, specialist, date/time, attempted action, and exact error text.

### 9) Quick Operational Playbooks (Scenario-Rewrite)

**Q: I am a first-time customer and need urgent (non-emergency) consultation. What exact sequence should I follow?**  
**A:** Search by category, set nearest date, sort by fee or level, open active specialist profile, select available slot, choose topic, add concise notes, submit, then monitor Pending -> Confirmed in My Bookings.

**Q: I need to move tomorrow’s appointment. What is the safest path?**  
**A:** Open Upcoming, try Reschedule, review quote, select new available slot, confirm, then wait again because status returns to Pending.

**Q: I clicked Confirm booking more than once and now I am worried. What should I do now?**  
**A:** Wait for processing and verify result in My Bookings; duplicate handling prevents repeated booking creation.

**Q: Specialist rejected my request and now it is cancelled. What can I do next?**  
**A:** Book another slot (if available) or choose another specialist.

**Q: I picked wrong topic in my booking details. How should I correct it?**  
**A:** If more than 2 hours before start, use Reschedule flow and submit with corrected topic; booking will return to Pending.

### 10) Canonical Guardrails for RAG Answering

Use these guardrails when generating answers from this document:

- Never say booking is immediately confirmed after submit; always say **Pending first**.
- Never allow booking of Booked/Unavailable slots.
- Never allow cancel/reschedule within 2 hours of start.
- Never state fixed cancellation fee without quote; fee is policy-calculated and shown in quote.
- Never claim reschedule can change specialist.
- Never claim reschedule keeps confirmed status; it returns to Pending.
- Never invent a separate checkout step on booking page.

This closes the expanded, policy-faithful FAQ rewrite.

### 11) High-Recall Query Variants (Search Intent Clusters)

**Q: How can I find, lookup, search, discover, or browse specialists quickly?**  
**A:** Use the specialist search page, set name/category/date/sort filters, then click **Search**.

**Q: How do I search by doctor name, expert name, consultant name, or partial keyword?**  
**A:** Enter full or partial text in **Expert name**, then click **Search**.

**Q: How do I filter by specialty, domain, department, or category?**  
**A:** Choose the target value in **Category** and run **Search**.

**Q: How do I find available specialists on a date, day, or exact schedule window?**  
**A:** Set **Available on** to that date, then search.

**Q: How do I rank by price, fee, cost, or consultation amount?**  
**A:** Use **Fee: Low to High** or **Fee: High to Low** in **Sort by**.

**Q: How do I prioritize seniority, level, rank, or experience tier?**  
**A:** Use **Level: High to Low** in **Sort by**.

**Q: Why does my search produce no matches, no cards, or empty results?**  
**A:** Filter combination is likely too narrow. Remove one or more constraints and retry.

**Q: Why did the specialist I saw earlier disappear from search?**  
**A:** Current filters may exclude that profile. Click **Reset** and search again.

**Q: How do I clear all filters, reset search state, and start over?**  
**A:** Click **Reset** on the search page.

**Q: How can I preserve current search conditions when returning from profile page?**  
**A:** Prefer **Back to results** and return via the same URL state.

### 12) High-Recall Query Variants (Booking Intent Clusters)

**Q: How do I create, submit, place, or send a booking request?**  
**A:** Pick date and **Available** slot, select **Topic**, optionally add **Notes**, then click **Confirm booking**.

**Q: What is mandatory before submission: slot, topic, notes, or all fields?**  
**A:** Slot and **Topic** are required; **Notes** are optional.

**Q: Can I submit with empty notes, blank notes, or no description text?**  
**A:** Yes. Notes are optional.

**Q: What happens immediately after successful booking request creation?**  
**A:** Booking appears in **My Bookings** with **Pending** status.

**Q: Is the time slot locked, reserved, held, or blocked after submit?**  
**A:** Yes. Once booking is created, the slot becomes booked.

**Q: Does booking creation equal final approval?**  
**A:** No. Created bookings start as **Pending** and need specialist approval.

**Q: Why did submit fail with time slot already booked?**  
**A:** The slot was taken before final submission. Choose another available slot.

**Q: Can repeated clicking create duplicate bookings?**  
**A:** Duplicate processing is prevented; wait and verify in My Bookings.

**Q: Where do I check result, response, or final status of submission?**  
**A:** In **My Bookings -> Upcoming**.

### 13) High-Recall Query Variants (Cancel/Reschedule Intent Clusters)

**Q: How do I modify, change, move, or adjust an existing appointment time?**  
**A:** Use **Reschedule** on an eligible booking in **Upcoming**.

**Q: How do I terminate, cancel, or withdraw a booking request?**  
**A:** Use **Cancel** on an eligible booking, review quote, then confirm.

**Q: Why do I see refund, penalty, and payable amounts before confirming?**  
**A:** Cancel/reschedule uses a mandatory **Quote -> Confirm** flow.

**Q: Which bookings are eligible for cancel or reschedule actions?**  
**A:** Only bookings with **Pending** or **Confirmed** status.

**Q: What blocks cancellation or rescheduling near appointment start time?**  
**A:** If within **2 hours** of start time, changes are blocked.

**Q: When is full-refund window active?**  
**A:** More than **24 hours** before start time.

**Q: When does penalty window apply?**  
**A:** Between **2 and 24 hours** before start time.

**Q: Can I reschedule to a different expert, doctor, or specialist?**  
**A:** No. Reschedule keeps the same specialist.

**Q: Why does status return to Pending after reschedule?**  
**A:** Reschedule creates a new approval request for the specialist.

**Q: Why is cancel/reschedule button disabled or greyed out?**  
**A:** Usually invalid status or start time within 2 hours.

### 14) Boundary and Policy Clarification FAQs (No New Rules)

**Q: Can I book a slot marked Booked if I wait and retry quickly?**  
**A:** No. Only **Available** slots are selectable.

**Q: Can I book a slot marked Unavailable by contacting support first?**  
**A:** No. **Unavailable** slots are not selectable for booking.

**Q: Can I change a Completed booking by any normal action button?**  
**A:** No. Change actions apply to **Pending** or **Confirmed** only.

**Q: Can I change a Cancelled booking into active again?**  
**A:** No. Cancelled status is not change-eligible.

**Q: Is there a hidden checkout page after clicking Confirm booking?**  
**A:** No. There is no separate online checkout step on booking page.

**Q: Are cancellation fees fixed by a static number in this guide?**  
**A:** No. Fees are dynamically calculated and shown in quote.

**Q: If quote says not allowed, can I force confirm anyway?**  
**A:** No. The quote result determines whether confirmation is allowed.

**Q: If specialist is inactive, can booking be queued until they return?**  
**A:** No. Inactive specialists cannot accept new bookings.

**Q: Is profile review count required to continue booking flow?**  
**A:** No. Main decision signals in current flow are category, level, status, fee, and bio.

**Q: Can I bypass topic selection by filling detailed notes?**  
**A:** No. **Topic** is required.

### 15) Troubleshooting Matrix (Symptom -> Likely Cause -> Recovery)

**Q: Symptom: Empty search results after multiple attempts. What is likely and what to do?**  
**A:** Likely cause is over-filtering. Recovery: clear date first, then broaden category/name and retry.

**Q: Symptom: Specialist card existed before but not now. How to recover?**  
**A:** Likely cause is current filter mismatch. Recovery: use **Reset**, then search by name only.

**Q: Symptom: Category dropdown appears blank. What should I do?**  
**A:** Likely temporary load/network issue. Recovery: refresh and retry.

**Q: Symptom: Slot looked available, then submit says booked. Why and next step?**  
**A:** Real-time inventory changed. Recovery: choose another available slot and submit again.

**Q: Symptom: Slot selection vanished after changing date. Is it expected?**  
**A:** Yes. Selection resets when date changes. Choose a new slot for that date.

**Q: Symptom: Topic list empty. What is safe recovery sequence?**  
**A:** Wait for loading once, then refresh and retry topic selection.

**Q: Symptom: Notes rejected for unsupported characters. What format works best?**  
**A:** Use standard text characters and concise wording.

**Q: Symptom: Confirm clicked repeatedly due to lag. What should I do now?**  
**A:** Stop clicking, wait for processing, verify booking in My Bookings.

**Q: Symptom: New booking not visible immediately. How to locate reliably?**  
**A:** Open **Upcoming**, set status to **All**, refresh once.

**Q: Symptom: Cancel/Reschedule action disabled unexpectedly. How to verify eligibility?**  
**A:** Check status is Pending/Confirmed and start time is more than 2 hours away.

**Q: Symptom: Reschedule final confirm failed. What likely happened?**  
**A:** Selected new slot was likely taken. Pick another available slot.

**Q: Symptom: Quote says not allowed even though user expected access. What then?**  
**A:** Re-check start time window and status. If still inconsistent, share booking ID and screenshot with support.

### 16) Pro Tips by Workflow Stage (Policy-Safe Optimization)

**Q: Pro-Tip for search precision without losing too many results?**  
**A:** Start with category only, then add date, then apply sorting. This avoids overly narrow first queries.

**Q: Pro-Tip for finding the most affordable valid option quickly?**  
**A:** Use category + date + **Fee: Low to High**, then shortlist active profiles only.

**Q: Pro-Tip for selecting between similar profiles?**  
**A:** Compare active status, category match, bio clarity, and fee in that order.

**Q: Pro-Tip for reducing booking submission failures?**  
**A:** Select slot first and submit soon after entering topic/notes to reduce inventory race risk.

**Q: Pro-Tip for note writing that improves context quality?**  
**A:** Keep three lines: current issue, duration, expected consultation outcome.

**Q: Pro-Tip for tracking approval updates efficiently?**  
**A:** Monitor **My Bookings -> Upcoming** with **Pending** and **Confirmed** filters.

**Q: Pro-Tip for changing appointment time with least friction?**  
**A:** If eligible, use **Reschedule** directly instead of canceling and creating a separate booking.

**Q: Pro-Tip for avoiding penalties where possible?**  
**A:** Act earlier than 24 hours before start to stay in full-refund window.

**Q: Pro-Tip for support tickets that get faster action?**  
**A:** Provide booking ID, specialist, slot time, exact action, and full error message text.

### 17) Additional Diverse User Questions (Extended Set)

**Q: Why does my booking show Pending even though slot is booked?**  
**A:** Slot reservation and specialist approval are different steps; Pending means awaiting approval.

**Q: Can I rely on a slot while I keep the booking form open for a long time?**  
**A:** Availability can change dynamically, so long delays increase risk of slot loss.

**Q: If I submit successfully, can another user still grab that same slot?**  
**A:** No. Once booking is created, that slot is booked.

**Q: Does sorting by fee change specialist status from inactive to active?**  
**A:** No. Sorting only changes list order.

**Q: Does changing date in search update profile availability checks automatically?**  
**A:** You still need to open the specialist booking view and inspect slot status.

**Q: Can I continue if profile has no direct contact info?**  
**A:** Yes. Contact disclosure is optional; booking is handled in-platform.

**Q: Why do I need to re-approve flow after reschedule if it is same specialist?**  
**A:** Because reschedule creates a fresh request state requiring specialist approval.

**Q: If I see generic booking error once, should I retry forever?**  
**A:** Retry once after refresh; if persistent, contact support with details.

**Q: Can I use History tab to monitor future pending approvals?**  
**A:** No. Use Upcoming for future and in-progress status tracking.

**Q: Is Book Again a direct confirmation action?**  
**A:** No. It navigates back so you can select a new slot and create a new request.

**Q: Why do I sometimes need to click slot twice?**  
**A:** UI state can lag briefly; re-click and confirm summary update.

**Q: Can a booking be both Confirmed and change-blocked?**  
**A:** Yes, if it is within 2 hours of start time.

**Q: If a specialist rejects, do I keep confirmed status?**  
**A:** No. Rejected request becomes Cancelled.

**Q: Is full-refund guaranteed at exactly 24-hour boundary?**  
**A:** Policy windows are defined as more than 24 hours for full-refund and 2-24 hours for penalty; quote displays actual calculation.

**Q: Can I estimate penalty without opening quote screen?**  
**A:** Final amounts are shown in the quote-confirm step.

**Q: Is there any valid path to edit booking under 2 hours before start?**  
**A:** Normal cancel/reschedule actions are blocked within 2 hours.

**Q: Why does my reschedule flow ask me to pick slot again?**  
**A:** Reschedule requires selecting a new available slot for same specialist.

**Q: Can I reschedule without changing time slot?**  
**A:** Reschedule flow is for choosing a new slot; if no change is needed, keep current booking.

**Q: Does a booked slot always mean confirmed booking?**  
**A:** Not necessarily; booking can be Pending while slot is already reserved.

**Q: Can I trust fee shown on profile for decision before booking?**  
**A:** Yes, fee is part of core decision signals in current flow.

**Q: Why do I need both search and profile view before booking?**  
**A:** Search narrows options; profile confirms status, fee, and suitability.

**Q: If category says unassigned, should I stop immediately?**  
**A:** You can continue evaluating by bio/fee, or choose a clearly labeled category profile.

**Q: What is fastest way to re-find a specialist later?**  
**A:** Search by name after noting specialist name from profile/history.

**Q: Can My Bookings show cancelled records?**  
**A:** Yes, in History.

**Q: Can My Bookings show pending future records?**  
**A:** Yes, in Upcoming.

**Q: Does quote step exist only for cancellation, not reschedule?**  
**A:** Quote step exists for both cancellation and rescheduling.

**Q: Is financial impact visible before final confirmation in both flows?**  
**A:** Yes. Refund/penalty/payable are shown before final confirm.

**Q: If I need a different specialist and different time, should I use reschedule?**  
**A:** No. Reschedule keeps specialist unchanged.

**Q: If filter state is lost, is that a booking bug?**  
**A:** Usually it is a navigation/state issue; use Back to results and URL-preserved state practices.

**Q: If I cannot find an urgent slot with one specialist, what is valid next step?**  
**A:** Check other specialists in same category with the same date filter.

### 18) RAG Safety: Forbidden Hallucinations and Canonical Answer Anchors

**Q: What must an AI assistant never hallucinate from this guide?**  
**A:** Never hallucinate immediate confirmation after booking submit, never allow booking non-available slots, never allow cancel/reschedule within 2 hours, never invent fixed fees, never invent cross-specialist rescheduling, and never invent a separate checkout page on booking step.

**Q: What should an AI assistant always anchor to for faithful answers?**  
**A:** Anchor to these constants: Pending-first workflow, slot status gating, quote-confirm requirement, timing policy windows, and same-specialist-only reschedule.

**Q: What is the default faithful answer when data is uncertain?**  
**A:** Ask user to verify in UI and quote screen while keeping policy statements strictly to the documented rules.

**Q: What is the safest phrasing for cancellation fee questions?**  
**A:** “Fees are policy-calculated dynamically and shown in your quote before confirmation.”

**Q: What is the safest phrasing for status progression questions?**  
**A:** “Booking is created as Pending and becomes Confirmed only after specialist approval.”

**Q: What is the safest phrasing for reschedule scope questions?**  
**A:** “Reschedule changes time slot for the same specialist and returns status to Pending.”

**Q: What is the safest phrasing for policy-window questions?**  
**A:** “Within 2 hours changes are blocked; more than 24 hours is full-refund window; 2-24 hours is penalty window.”
