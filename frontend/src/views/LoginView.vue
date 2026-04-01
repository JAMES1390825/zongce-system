<template>
  <div class="card login-card" style="width: 100%;">
    <p class="page-eyebrow">Campus Evaluation Hub</p>
    <h2 class="login-title">综测系统登录</h2>
    <p class="muted login-subtitle">请输入系统分配的账号和密码</p>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <form @submit.prevent="onSubmit">
      <label>账号</label>
      <input v-model.trim="username" required autocomplete="username" />

      <label>密码</label>
      <input
        v-model="password"
        required
        type="password"
        autocomplete="current-password"
      />

      <button type="submit" :disabled="auth.state.loading">
        {{ auth.state.loading ? '登录中...' : '登录' }}
      </button>
    </form>

  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '../lib/auth';

const router = useRouter();
const auth = useAuth();

const username = ref('');
const password = ref('');
const error = ref('');

async function onSubmit() {
  error.value = '';
  try {
    await auth.login(username.value, password.value);
    await router.push('/dashboard');
  } catch (e) {
    error.value = e.message || '登录失败';
  }
}
</script>
