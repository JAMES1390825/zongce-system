import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './token';

const API_BASE = import.meta.env.VITE_API_BASE || '';

let refreshPromise = null;
let authExpiredHandler = null;

function notifyAuthExpired() {
  if (typeof authExpiredHandler === 'function') {
    authExpiredHandler();
  }
}

export function setAuthExpiredHandler(handler) {
  authExpiredHandler = handler;
}

async function requestRaw(path, options = {}) {
  const { method = 'GET', data, isForm = false, accessToken } = options;
  const fetchOptions = {
    method,
    credentials: 'include',
    headers: {}
  };

  if (accessToken) {
    fetchOptions.headers.Authorization = `Bearer ${accessToken}`;
  }

  if (data !== undefined) {
    if (isForm) {
      fetchOptions.body = data;
    } else {
      fetchOptions.headers['Content-Type'] = 'application/json';
      fetchOptions.body = JSON.stringify(data);
    }
  }

  const response = await fetch(`${API_BASE}${path}`, fetchOptions);
  const contentType = response.headers.get('content-type') || '';
  let payload = null;

  if (contentType.includes('application/json')) {
    payload = await response.json();
  } else {
    const text = await response.text();
    payload = { message: text };
  }

  return { response, payload };
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('登录已过期，请重新登录');
  }

  if (!refreshPromise) {
    refreshPromise = (async () => {
      const { response, payload } = await requestRaw('/api/auth/refresh', {
        method: 'POST',
        data: { refreshToken }
      });
      if (!response.ok) {
        throw new Error(payload?.message || '登录已过期，请重新登录');
      }
      if (payload?.accessToken) {
        setTokens(payload.accessToken, payload.refreshToken || refreshToken);
      }
      return payload;
    })().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

export async function request(path, options = {}) {
  const accessToken = getAccessToken();
  const { response, payload } = await requestRaw(path, {
    ...options,
    accessToken
  });

  if (response.status === 401
      && !options.__retry
      && !path.startsWith('/api/auth/login')
      && !path.startsWith('/api/auth/refresh')) {
    try {
      await refreshAccessToken();
      return request(path, { ...options, __retry: true });
    } catch (e) {
      clearTokens();
      notifyAuthExpired();
      throw new Error(e.message || '登录已过期，请重新登录');
    }
  }

  if (!response.ok) {
    throw new Error(payload?.message || `请求失败 (${response.status})`);
  }

  return payload;
}
