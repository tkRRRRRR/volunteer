import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, updateProfile as updateProfileApi } from '../api/auth'

const TOKEN_KEY = 'volunteer_token'
const USER_KEY = 'volunteer_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN',
    displayName: (state) => state.user?.nickname || state.user?.username || '同学'
  },
  actions: {
    persist(payload) {
      this.token = payload.token
      this.user = payload.user
      localStorage.setItem(TOKEN_KEY, payload.token)
      localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
    },
    async login(form) {
      const payload = await loginApi(form)
      this.persist(payload)
    },
    async register(form) {
      const payload = await registerApi(form)
      this.persist(payload)
    },
    async updateProfile(form) {
      this.user = await updateProfileApi(form)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
