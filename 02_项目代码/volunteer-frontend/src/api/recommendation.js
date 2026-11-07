import request from '../utils/request'

export const createRecommendation = (data) => request.post('/recommendations', data)
