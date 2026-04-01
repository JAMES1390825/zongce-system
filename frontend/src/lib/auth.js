import { computed, reactive } from 'vue';
import { request, setAuthExpiredHandler } from './http';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './token';

const state = reactive({
  user: null,
  ready: false,
  loading: false
});

let initPromise = null;

function resetAuthState(resetInitPromise = true) {
  clearTokens();
  state.user = null;
  state.ready = true;
  if (resetInitPromise) {
    initPromise = null;
  }
}

setAuthExpiredHandler(() => {
  resetAuthState(true);
});

async function fetchMe() {
  try {
    const res = await request('/api/auth/me');
    state.user = res.user;
  } catch (error) {
    resetAuthState(false);
  } finally {
    state.ready = true;
  }
}

async function login(username, password) {
  state.loading = true;
  try {
    const res = await request('/api/auth/login', {
      method: 'POST',
      data: { username, password }
    });
    if (res?.accessToken) {
      setTokens(res.accessToken, res.refreshToken || '');
    }
    await fetchMe();
  } finally {
    state.loading = false;
  }
}

async function logout() {
  try {
    await request('/api/auth/logout', { method: 'POST' });
  } finally {
    resetAuthState(true);
  }
}

async function changePassword(oldPassword, newPassword) {
  return request('/api/auth/password', {
    method: 'PUT',
    data: { oldPassword, newPassword }
  });
}

function initAuth() {
  const hasToken = !!(getAccessToken() || getRefreshToken());
  if (!hasToken && state.user) {
    state.user = null;
    state.ready = true;
    initPromise = Promise.resolve();
    return initPromise;
  }

  if (!initPromise) {
    initPromise = fetchMe();
  }
  return initPromise;
}

export function useAuth() {
  return {
    state,
    isLoggedIn: computed(() => !!state.user),
    role: computed(() => state.user?.role || ''),
    fetchMe,
    initAuth,
    login,
    logout,
    changePassword
  };
}

export { state as authState, fetchMe, initAuth, login, logout, changePassword };
