import request from '../utils/request'

export const getUniversities = (params) => request.get('/universities', { params })
export const getUniversity = (id) => request.get(`/universities/${id}`)
export const createUniversity = (data) => request.post('/admin/universities', data)
export const updateUniversity = (id, data) => request.put(`/admin/universities/${id}`, data)
export const deleteUniversity = (id) => request.delete(`/admin/universities/${id}`)
