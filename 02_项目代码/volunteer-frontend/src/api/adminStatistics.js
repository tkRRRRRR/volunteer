import request from '../utils/request'

export const getAdminStatistics = () =>
  request.get('/admin/statistics/overview')

export const getAdminStudents = (params) =>
  request.get('/admin/students', { params })
