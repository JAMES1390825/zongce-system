<template>
  <div>
    <div class="card">
      <h2>系统日志</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
    </div>

    <div class="card">
      <h3>登录日志</h3>
      <form class="inline" @submit.prevent="loadLoginLogs">
        <input v-model.trim="loginUsername" placeholder="账号关键字" style="width: 180px; margin-bottom: 0;" />
        <select v-model="loginSuccess" style="width: 140px; margin-bottom: 0;">
          <option value="">全部</option>
          <option value="true">成功</option>
          <option value="false">失败</option>
        </select>
        <button type="submit">查询</button>
      </form>

      <table>
        <thead>
          <tr>
            <th>账号</th>
            <th>结果</th>
            <th>IP</th>
            <th>信息</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in loginLogs" :key="row.id">
            <td>{{ row.username || '-' }}</td>
            <td>{{ row.success ? '成功' : '失败' }}</td>
            <td>{{ row.clientIp || '-' }}</td>
            <td>{{ row.message || '-' }}</td>
            <td>{{ row.createdAt || '-' }}</td>
          </tr>
          <tr v-if="loginLogs.length === 0">
            <td colspan="5" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3>审计日志</h3>
      <form class="inline" @submit.prevent="loadAuditLogs">
        <input v-model.trim="auditUsername" placeholder="账号关键字" style="width: 180px; margin-bottom: 0;" />
        <input v-model.trim="auditModule" placeholder="模块，如 admin" style="width: 180px; margin-bottom: 0;" />
        <select v-model="auditSuccess" style="width: 140px; margin-bottom: 0;">
          <option value="">全部</option>
          <option value="true">成功</option>
          <option value="false">失败</option>
        </select>
        <button type="submit">查询</button>
      </form>

      <table>
        <thead>
          <tr>
            <th>账号</th>
            <th>模块</th>
            <th>动作</th>
            <th>结果</th>
            <th>详情</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in auditLogs" :key="row.id">
            <td>{{ row.username || '-' }}</td>
            <td>{{ row.moduleName }}</td>
            <td>{{ row.actionName }}</td>
            <td>{{ row.success ? '成功' : '失败' }}</td>
            <td>{{ row.detail || '-' }}</td>
            <td>{{ row.createdAt || '-' }}</td>
          </tr>
          <tr v-if="auditLogs.length === 0">
            <td colspan="6" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../../lib/http';

const error = ref('');

const loginUsername = ref('');
const loginSuccess = ref('');
const loginLogs = ref([]);

const auditUsername = ref('');
const auditModule = ref('');
const auditSuccess = ref('');
const auditLogs = ref([]);

async function loadLoginLogs() {
  try {
    error.value = '';
    const params = new URLSearchParams();
    if (loginUsername.value) params.append('username', loginUsername.value);
    if (loginSuccess.value) params.append('success', loginSuccess.value);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/api/admin/login-logs${query}`);
    loginLogs.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载登录日志失败';
  }
}

async function loadAuditLogs() {
  try {
    error.value = '';
    const params = new URLSearchParams();
    if (auditUsername.value) params.append('username', auditUsername.value);
    if (auditModule.value) params.append('module', auditModule.value);
    if (auditSuccess.value) params.append('success', auditSuccess.value);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/api/admin/audit-logs${query}`);
    auditLogs.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载审计日志失败';
  }
}

onMounted(async () => {
  await Promise.all([loadLoginLogs(), loadAuditLogs()]);
});
</script>
