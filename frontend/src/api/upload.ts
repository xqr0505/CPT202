import request from './request'

export interface UploadImageResponse {
  url: string
}

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request.post<any, UploadImageResponse>('/admin/uploads/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
