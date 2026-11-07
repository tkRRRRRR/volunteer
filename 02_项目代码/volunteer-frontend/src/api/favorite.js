import request from '../utils/request'

export const getFavorites = () => request.get('/favorites')
export const addFavorite = (data) => request.post('/favorites', data)
export const removeFavorite = (id) => request.delete(`/favorites/${id}`)
