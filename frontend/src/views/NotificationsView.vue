<template>
  <div class="page-shell">
    <section class="card">
      <div class="inline" style="justify-content: space-between; align-items: flex-start; width: 100%;">
        <div>
          <h2 style="margin: 0 0 8px;">通知中心</h2>
          <p class="muted" style="margin: 0;">未读数量：{{ unreadCount }}（每 30 秒自动刷新）</p>
        </div>
        <button type="button" class="secondary" @click="goDashboard">返回首页</button>
      </div>

      <div v-if="error" class="alert alert-error" style="margin-top: 12px;">{{ error }}</div>
      <div v-if="success" class="alert alert-success" style="margin-top: 12px;">{{ success }}</div>

      <form class="inline" style="margin-top: 12px;" @submit.prevent="loadRows">
        <label style="margin-bottom: 0;">仅看未读</label>
        <select v-model="unreadOnly" style="width: 120px; margin-bottom: 0;">
          <option :value="false">否</option>
          <option :value="true">是</option>
        </select>
        <button type="submit" :disabled="loading">{{ loading ? '加载中...' : '刷新' }}</button>
        <button type="button" class="secondary" @click="markAllRead" :disabled="loading || actionLoadingAll">
          {{ actionLoadingAll ? '处理中...' : '批量已读' }}
        </button>
        <button type="button" class="danger" @click="deleteRead" :disabled="loading || actionLoadingAll">
          {{ actionLoadingAll ? '处理中...' : '删除已读' }}
        </button>
      </form>
    </section>

    <section class="card">
      <div v-if="loading" class="muted">正在加载通知...</div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>状态</th>
              <th>标题</th>
              <th>内容</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>
                <span :class="row.readFlag ? 'badge badge-approved' : 'badge badge-pending'">
                  {{ row.readFlag ? '已读' : '未读' }}
                </span>
              </td>
              <td>{{ row.title }}</td>
              <td>{{ row.content }}</td>
              <td>{{ formatTime(row.createdAt) }}</td>
              <td class="inline">
                <button
                  v-if="!row.readFlag"
                  type="button"
                  @click="markRead(row.id)"
                  :disabled="actionLoadingId === row.id"
                >
                  {{ actionLoadingId === row.id ? '处理中...' : '标已读' }}
                </button>
                <button
                  type="button"
                  class="secondary"
                  @click="openTarget(row)"
                  :disabled="actionLoadingId === row.id"
                >查看</button>
                <button
                  type="button"
                  class="danger"
                  @click="deleteOne(row.id)"
                  :disabled="actionLoadingId === row.id"
                >删除</button>
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="5" class="muted">暂无通知</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { request } from '../lib/http';

const router = useRouter();
const rows = ref([]);
const unreadCount = ref(0);
const unreadOnly = ref(false);
const error = ref('');
const success = ref('');
const loading = ref(false);
const actionLoadingId = ref(null);
const actionLoadingAll = ref(false);

let refreshTimer = null;

function emitNotificationsUpdated() {
  window.dispatchEvent(new Event('notifications-updated'));
}

function setMessage(type, text) {
  if (type === 'error') {
    error.value = text;
    success.value = '';
  } else {
    success.value = text;
    error.value = '';
  }
}

function formatTime(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').slice(0, 19);
}

async function goDashboard() {
  if (router.currentRoute.value.path !== '/dashboard') {
    await router.push('/dashboard');
  }
}

async function loadRows() {
  try {
    loading.value = true;
    error.value = '';
    const query = unreadOnly.value ? '?unreadOnly=true' : '';
    const res = await request(`/api/notifications${query}`);
    rows.value = res.data || [];
    unreadCount.value = res.unreadCount || 0;
  } catch (e) {
    setMessage('error', e.message || '加载通知失败');
  } finally {
    loading.value = false;
  }
}

async function markRead(id) {
  try {
    actionLoadingId.value = id;
    const res = await request(`/api/notifications/${id}/read`, { method: 'POST' });
    setMessage('success', res.message || '操作成功');
    await loadRows();
    emitNotificationsUpdated();
  } catch (e) {
    setMessage('error', e.message || '操作失败');
  } finally {
    actionLoadingId.value = null;
  }
}

async function openTarget(row) {
  if (!row) {
    return;
  }
  if (!row.readFlag) {
    await markRead(row.id);
  }
  const target = row.linkUrl || '/notifications';
  if (router.currentRoute.value.path !== target) {
    await router.push(target);
  }
}

async function deleteOne(id) {
  if (!window.confirm('确认删除这条通知吗？')) {
    return;
  }
  try {
    actionLoadingId.value = id;
    const res = await request(`/api/notifications/${id}`, { method: 'DELETE' });
    setMessage('success', res.message || '删除成功');
    await loadRows();
    emitNotificationsUpdated();
  } catch (e) {
    setMessage('error', e.message || '删除失败');
  } finally {
    actionLoadingId.value = null;
  }
}

async function markAllRead() {
  if (!window.confirm('确认将当前账号所有通知标记为已读吗？')) {
    return;
  }

  try {
    actionLoadingAll.value = true;
    const res = await request('/api/notifications/read-all', { method: 'POST' });
    setMessage('success', res.message || '操作成功');
    await loadRows();
    emitNotificationsUpdated();
  } catch (e) {
    setMessage('error', e.message || '操作失败');
  } finally {
    actionLoadingAll.value = false;
  }
}

async function deleteRead() {
  if (!window.confirm('确认删除所有已读通知吗？')) {
    return;
  }

  try {
    actionLoadingAll.value = true;
    const res = await request('/api/notifications/delete-read', { method: 'POST' });
    setMessage('success', res.message || '操作成功');
    await loadRows();
    emitNotificationsUpdated();
  } catch (e) {
    setMessage('error', e.message || '操作失败');
  } finally {
    actionLoadingAll.value = false;
  }
}

function onNotificationsUpdated() {
  void loadRows();
}

onMounted(async () => {
  window.addEventListener('notifications-updated', onNotificationsUpdated);
  await loadRows();
  refreshTimer = window.setInterval(loadRows, 30000);
});

onBeforeUnmount(() => {
  window.removeEventListener('notifications-updated', onNotificationsUpdated);
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
  }
});
</script>
