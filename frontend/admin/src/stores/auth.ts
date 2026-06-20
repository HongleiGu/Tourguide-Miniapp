import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'tg_admin_token'
const USER_KEY = 'tg_admin_user'

/** Auth state. The real login flow (wx/JWT, roles) is wired in MIN-2; this is a dev stub. */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const username = ref<string>(localStorage.getItem(USER_KEY) ?? '')

  const isAuthenticated = computed(() => token.value.length > 0)

  function login(name: string, newToken: string) {
    token.value = newToken
    username.value = name
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, name)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, username, isAuthenticated, login, logout }
})
