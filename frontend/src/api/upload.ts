import request from './request'

export interface UploadImageResponse {
  url: string
}

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file, file.name)

  return request.post<any, UploadImageResponse>('/v1/admin/specialists/avatar', formData)
}
