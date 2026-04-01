import request from './request'

// ============== Types ==============
export interface TimeSlotVO {
  id: number
  specialistId: number
  slotDate: string
  startTime: string
  endTime: string
  status: string
  recurringRuleId?: number
  createdAt: string
}

export interface CreateSlotRequest {
  slotDate: string
  startTime: string
  endTime: string
}

export interface UpdateSlotRequest {
  startTime?: string
  endTime?: string
  status?: string
}

export interface RecurringRuleVO {
  id: number
  specialistId: number
  dayOfWeek: number
  dayOfWeekDesc: string
  startTime: string
  endTime: string
  effectiveEndDate: string
  isActive: number
  statusDesc: string
  createdAt: string
}

export interface CreateRecurringRuleRequest {
  dayOfWeek: number
  startTime: string
  endTime: string
  effectiveEndDate: string
}

// ============== Time Slot APIs ==============
export const getWeeklySchedule = (weekStartDate?: string): Promise<TimeSlotVO[]> => {
  const params = weekStartDate ? { weekStartDate } : {}
  return request.get('/api/specialist/schedule/slots/weekly', { params })
}

export const getSlotById = (id: number): Promise<TimeSlotVO> => {
  return request.get(`/api/specialist/schedule/slots/${id}`)
}

export const createSlot = (data: CreateSlotRequest): Promise<TimeSlotVO> => {
  return request.post('/api/specialist/schedule/slots', data)
}

export const updateSlot = (id: number, data: UpdateSlotRequest): Promise<TimeSlotVO> => {
  return request.put(`/api/specialist/schedule/slots/${id}`, data)
}

export const deleteSlot = (id: number): Promise<void> => {
  return request.delete(`/api/specialist/schedule/slots/${id}`)
}

// ============== Recurring Rule APIs ==============
export const getAllRecurringRules = (): Promise<RecurringRuleVO[]> => {
  return request.get('/api/specialist/schedule/rules')
}

export const getActiveRecurringRules = (): Promise<RecurringRuleVO[]> => {
  return request.get('/api/specialist/schedule/rules/active')
}

export const createRecurringRule = (data: CreateRecurringRuleRequest): Promise<RecurringRuleVO> => {
  return request.post('/api/specialist/schedule/rules', data)
}

export const deleteRecurringRule = (id: number): Promise<void> => {
  return request.delete(`/api/specialist/schedule/rules/${id}`)
}
