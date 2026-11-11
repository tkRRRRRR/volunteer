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
