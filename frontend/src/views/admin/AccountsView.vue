<template>
  <div>
    <div class="card">
      <h2>账号管理</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <form class="inline" @submit.prevent="loadRows">
        <input
          v-model.trim="keyword"
          placeholder="按账号/姓名/角色/班级/部门搜索"
          style="width: 260px; margin-bottom: 0;"
        />
        <select v-model="filterRole" style="width: 150px; margin-bottom: 0;">
          <option value="">全部角色</option>
          <option v-for="r in roles" :key="r" :value="r">{{ r }}</option>
        </select>
        <select v-model="filterEnabled" style="width: 130px; margin-bottom: 0;">
          <option value="">全部状态</option>
          <option value="true">启用</option>
          <option value="false">禁用</option>
        </select>
        <button type="submit">查询</button>
        <button type="button" class="secondary" @click="clearFilter">清空</button>
      </form>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>账号</th>
            <th>姓名</th>
            <th>角色</th>
            <th>班级</th>
            <th>部门</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.username }}</td>
            <td>{{ row.name }}</td>
            <td>{{ row.role }}</td>
            <td>{{ row.className || '-' }}</td>
            <td>{{ row.department || '-' }}</td>
            <td>
              <span class="badge" :class="row.enabled ? 'badge-approved' : 'badge-rejected'">
                {{ row.enabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="inline">
              <button v-if="row.enabled" class="secondary" @click="changeStatus(row, false)">禁用</button>
              <button v-else @click="changeStatus(row, true)">启用</button>
              <button class="secondary" @click="resetPassword(row.id)">重置密码</button>
            </td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="8" class="muted">暂无账号数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../../lib/http';

const roles = ['ADMIN', 'TEACHER_PE', 'TEACHER_STUDY', 'COUNSELOR', 'STUDENT'];
const rows = ref([]);
const keyword = ref('');
const filterRole = ref('');
const filterEnabled = ref('');
const error = ref('');
const success = ref('');

function setMessage(type, text) {
  if (type === 'error') {
    error.value = text;
    success.value = '';
  } else {
    success.value = text;
    error.value = '';
  }
}

async function loadRows() {
  try {
    const params = new URLSearchParams();
    if (keyword.value) params.append('keyword', keyword.value);
    if (filterRole.value) params.append('role', filterRole.value);
    if (filterEnabled.value) params.append('enabled', filterEnabled.value);
    const query = params.toString() ? `?${params.toString()}` : '';

    const res = await request(`/api/admin/accounts${query}`);
    rows.value = res.data || [];
  } catch (e) {
    setMessage('error', e.message || '加载失败');
  }
}

function clearFilter() {
  keyword.value = '';
  filterRole.value = '';
  filterEnabled.value = '';
  loadRows();
}

async function changeStatus(row, enabled) {
  try {
    const res = await request(`/api/admin/accounts/${row.id}/status`, {
      method: 'PUT',
      data: { enabled }
    });
    setMessage('success', res.message || '状态已更新');
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '更新失败');
  }
}

async function resetPassword(id) {
  if (!window.confirm('确认将该账号密码重置为系统初始密码吗？')) {
    return;
  }
  try {
    const res = await request(`/api/admin/accounts/${id}/reset-password`, { method: 'POST' });
    setMessage('success', res.message || '重置成功');
  } catch (e) {
    setMessage('error', e.message || '重置失败');
  }
}

onMounted(loadRows);
</script>
