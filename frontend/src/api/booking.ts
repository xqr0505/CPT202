import request from './request'

const USE_MOCK = false

export interface SpecialistPendingBookingVO {
  id: number
  customerName: string
  requestedStartTime: string
  requestedEndTime: string
  topic: string
  submissionTime: string
  customerNotes?: string
}

export interface SpecialistHandledBookingVO {
  id: number
  customerName: string
  requestedStartTime: string
  requestedEndTime: string
  topic: string
  submissionTime: string
  customerNotes?: string
  status: 'APPROVED' | 'REJECTED'
  decisionTime: string
  rejectionReason?: string
}

export interface SpecialistBookingDetailVO {
  id: number
  customerName: string
  requestedStartTime: string
  requestedEndTime: string
  topic: string
  submissionTime: string
  customerNotes?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  decisionTime?: string
  rejectionReason?: string
}

let mockPendingRequests: SpecialistPendingBookingVO[] = [
  {
    id: 101,
    customerName: 'Alice Zhang',
    requestedStartTime: '2026-04-05T10:00:00',
    requestedEndTime: '2026-04-05T11:00:00',
    topic: 'Career Planning',
    submissionTime: '2026-04-02T09:15:00',
    customerNotes: 'I want advice on switching from software testing to backend development.'
  },
  {
    id: 102,
    customerName: 'Bob Li',
    requestedStartTime: '2026-04-06T14:00:00',
    requestedEndTime: '2026-04-06T15:00:00',
    topic: 'Interview Preparation',
    submissionTime: '2026-04-02T11:30:00',
    customerNotes: 'I need help preparing for Java backend interviews.'
  }
]

let mockHandledRequests: SpecialistHandledBookingVO[] = [
  {
    id: 88,
    customerName: 'Cathy Wang',
    requestedStartTime: '2026-03-28T09:00:00',
    requestedEndTime: '2026-03-28T10:00:00',
    topic: 'CV Review',
    submissionTime: '2026-03-25T16:20:00',
    customerNotes: 'Please review my CV for internship applications.',
    status: 'APPROVED',
    decisionTime: '2026-03-26T10:05:00'
  },
  {
    id: 89,
    customerName: 'David Chen',
    requestedStartTime: '2026-03-29T15:00:00',
    requestedEndTime: '2026-03-29T16:00:00',
    topic: 'Graduate Study Advice',
    submissionTime: '2026-03-25T18:10:00',
    customerNotes: 'I want to discuss master programme applications.',
    status: 'REJECTED',
    decisionTime: '2026-03-26T08:40:00',
    rejectionReason: 'I am unavailable during the requested period.'
  }
]

export const getPendingBookingRequests = (): Promise<SpecialistPendingBookingVO[]> => {
  if (USE_MOCK) {
    return Promise.resolve([...mockPendingRequests])
  }
  return request.get('/api/v1/specialist/booking-requests/pending')
}

export const getHandledBookingRequests = (): Promise<SpecialistHandledBookingVO[]> => {
  if (USE_MOCK) {
    return Promise.resolve([...mockHandledRequests])
  }
  return request.get('/api/v1/specialist/booking-requests/history')
}

export const getBookingRequestDetail = (
  id: number
): Promise<SpecialistBookingDetailVO> => {
  if (USE_MOCK) {
    const pending = mockPendingRequests.find(item => item.id === id)
    if (pending) {
      return Promise.resolve({
        ...pending,
        status: 'PENDING'
      })
    }

    const handled = mockHandledRequests.find(item => item.id === id)
    if (handled) {
      return Promise.resolve({
        ...handled
      })
    }

    return Promise.reject(new Error('Booking request not found'))
  }

  return request.get(`/api/v1/specialist/booking-requests/${id}`)
}

export const approveBookingRequest = (id: number): Promise<void> => {
  if (USE_MOCK) {
    const index = mockPendingRequests.findIndex(item => item.id === id)
    if (index === -1) {
      return Promise.reject(new Error('Pending booking request not found'))
    }

    const item = mockPendingRequests[index]!
    mockPendingRequests.splice(index, 1)

    mockHandledRequests.unshift({
      ...item,
      status: 'APPROVED',
      decisionTime: new Date().toISOString()
    })

    return Promise.resolve()
  }

  return request.post(`/api/v1/specialist/booking-requests/${id}/approve`)
}

export const rejectBookingRequest = (
  id: number,
  rejectionReason: string
): Promise<void> => {
  if (USE_MOCK) {
    if (!rejectionReason.trim()) {
      return Promise.reject(new Error('Rejection reason is required'))
    }

    const index = mockPendingRequests.findIndex(item => item.id === id)
    if (index === -1) {
      return Promise.reject(new Error('Pending booking request not found'))
    }

    const item = mockPendingRequests[index]!
    mockPendingRequests.splice(index, 1)

    mockHandledRequests.unshift({
      ...item,
      status: 'REJECTED',
      decisionTime: new Date().toISOString(),
      rejectionReason
    })

    return Promise.resolve()
  }

  return request.post(`/api/v1/specialist/booking-requests/${id}/reject`, {
    rejectionReason
  })
}
