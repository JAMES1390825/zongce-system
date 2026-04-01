const ACCESS_KEY = 'zongce_access_token';
const REFRESH_KEY = 'zongce_refresh_token';

function safeGet(key) {
  try {
    return window.localStorage.getItem(key) || '';
  } catch (e) {
    return '';
  }
}

function safeSet(key, value) {
  try {
    if (!value) {
      window.localStorage.removeItem(key);
      return;
    }
    window.localStorage.setItem(key, value);
  } catch (e) {
    // ignore
  }
}

export function getAccessToken() {
  return safeGet(ACCESS_KEY);
}

export function getRefreshToken() {
  return safeGet(REFRESH_KEY);
}

export function setTokens(accessToken, refreshToken) {
  safeSet(ACCESS_KEY, accessToken || '');
  safeSet(REFRESH_KEY, refreshToken || '');
}

export function clearTokens() {
  safeSet(ACCESS_KEY, '');
  safeSet(REFRESH_KEY, '');
}
