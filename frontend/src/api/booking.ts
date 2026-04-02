import request from './request'

export interface UpcomingBookingResponse {
  id: string;
  specialistName: string;
  serviceName: string;
  startTime: string; // format: 'YYYY-MM-DD HH:mm:ss'
  today: boolean;
  status: string;
}

export const getUpcomingBookings = () => {
  return request.get<UpcomingBookingResponse[]>('/api/v1/customer/dashboard/upcoming')
}
