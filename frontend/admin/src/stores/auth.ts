import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const ACCESS_KEY = 'tg_access'
const REFRESH_KEY = 'tg_refresh'
const USER_KEY = 'tg_user'
const ROLES_KEY = 'tg_roles'

/** Admin auth state, persisted to localStorage so a refresh keeps the session. */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string>(localStorage.getItem(ACCESS_KEY) ?? '')
  const refreshToken = ref<string>(localStorage.getItem(REFRESH_KEY) ?? '')
  const username = ref<string>(localStorage.getItem(USER_KEY) ?? '')
  const roles = ref<string[]>(JSON.parse(localStorage.getItem(ROLES_KEY) ?? '[]'))

  const isAuthenticated = computed(() => accessToken.value.length > 0)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem(ACCESS_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
  }

  function setProfile(name: string, userRoles: string[]) {
    username.value = name
    roles.value = userRoles
    localStorage.setItem(USER_KEY, name)
    localStorage.setItem(ROLES_KEY, JSON.stringify(userRoles))
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    username.value = ''
    roles.value = []
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(ROLES_KEY)
  }

  return { accessToken, refreshToken, username, roles, isAuthenticated, setTokens, setProfile, hasRole, logout }
})
