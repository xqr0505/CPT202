---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

# Bug Report: [Short Description of the Bug]

## 1. Identification
- **Module:** [e.g., Module 7]
- **Environment:** [ ] Frontend | [ ] Backend | [ ] Database
- **Severity:** [ ] Critical (Blocker) | [ ] Major (Broken logic) | [ ] Minor (Visual/Glitch)
- **Assignee (Module Owner):** [@Owner Name]
- **Reporter:** [@Your Name]

---

## 2. Description
*What is the bug? Provide a clear and concise summary.*
> [e.g., The 'Cancel' button throws a 500 Error when clicked.]

---

## 3. Steps to Reproduce
*How can we see this bug?*
1. Login as 'Specialist'
2. Navigate to 'My Appointments'
3. Click on the 'Cancel' button for any pending booking
4. Observe the error

---

## 4. Expected vs. Actual Behavior
- **Expected:** The status should change to 'Cancelled' and a success message should appear.
- **Actual:** The page freezes, and the browser console shows a `500 Internal Server Error`.

---

## 5. Attachments & Logs
*Paste screenshots, screen recordings, or API response logs here.*
- **API Endpoint:** `POST /api/v1/bookings/cancel`
- **Error Log:** `[Paste backend stack trace or console error here]`
- **Screenshot:** [Drop image here]

---

## 6. Possible Cause (Optional)
*If you have a guess on why this is happening:*
> [e.g., Maybe the `booking_id` is being sent as a String instead of a Long in the JSON payload?]
