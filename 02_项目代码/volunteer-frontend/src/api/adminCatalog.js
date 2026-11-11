import request from '../utils/request'

export const listMajors = (universityId, params) =>
  request.get(`/admin/universities/${universityId}/majors`, { params })

export const createMajor = (universityId, data) =>
  request.post(`/admin/universities/${universityId}/majors`, data)

export const updateMajor = (universityId, majorId, data) =>
  request.put(`/admin/universities/${universityId}/majors/${majorId}`, data)

export const deleteMajor = (universityId, majorId) =>
  request.delete(`/admin/universities/${universityId}/majors/${majorId}`)

export const getAdmissionRecords = (universityId, params) =>
  request.get(`/admin/universities/${universityId}/admission-records`, { params })

export const createAdmissionRecord = (universityId, data) =>
  request.post(`/admin/universities/${universityId}/admission-records`, data)

export const updateAdmissionRecord = (universityId, recordId, data) =>
  request.put(`/admin/universities/${universityId}/admission-records/${recordId}`, data)

export const deleteAdmissionRecord = (universityId, recordId) =>
  request.delete(`/admin/universities/${universityId}/admission-records/${recordId}`)

export const importAdmissionRecords = (universityId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(
    `/admin/universities/${universityId}/admission-records/import`,
    formData
  )
}

export const downloadAdmissionImportTemplate = async (universityId) => {
  const token = localStorage.getItem('volunteer_token')
  const response = await fetch(
    `/api/admin/universities/${universityId}/admission-records/import-template`,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  )
  if (!response.ok) {
    let message = 'Excel 模板下载失败'
    try {
      const payload = await response.json()
      message = payload.message || message
    } catch {
      // 二进制接口异常时可能没有 JSON 响应，保留统一中文提示。
    }
    throw new Error(message)
  }

  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = '录取记录导入模板.xlsx'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(objectUrl)
}
