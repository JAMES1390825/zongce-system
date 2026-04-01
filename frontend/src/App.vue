<template>
  <div>
    <div v-if="!auth.state.ready" class="loading-screen">正在加载登录状态...</div>

    <template v-else>
      <header v-if="showLayout" class="topbar">
        <div class="inner">
          <div class="topbar-left">
            <RouterLink class="brand-mark" to="/dashboard">综测系统</RouterLink>

            <nav class="menu">
              <RouterLink to="/dashboard">首页</RouterLink>
              <RouterLink
                v-for="item in menuItems"
                :key="item.to"
                :to="item.to"
              >
                {{ item.label }}
              </RouterLink>
            </nav>
          </div>

          <div class="topbar-right" v-if="auth.state.user">
            <div class="notice-menu" ref="noticeMenuRef">
              <button
                type="button"
                class="notice-trigger"
                @click="toggleNoticeMenu"
                :aria-expanded="noticeMenuOpen"
                title="通知"
              >
                <span class="notice-icon">✉</span>
                <span v-if="noticeUnreadCount > 0" class="notice-badge">{{ noticeBadgeText }}</span>
              </button>

              <section v-if="noticeMenuOpen" class="notice-dropdown">
                <header class="notice-head">
                  <strong>通知</strong>
                  <span class="muted">未读 {{ noticeUnreadCount }}</span>
                </header>

                <div v-if="noticeLoading" class="muted">正在加载通知...</div>

                <div v-else>
                  <button
                    v-for="row in noticeRows"
                    :key="row.id"
                    type="button"
                    class="notice-item"
                    @click="openNoticeDetail(row)"
                  >
                    <p class="notice-item-title">
                      <span v-if="!row.readFlag" class="dot-unread"></span>
                      {{ row.title }}
                    </p>
                    <p class="notice-item-content">{{ row.content }}</p>
                    <small class="muted">{{ formatNoticeTime(row.createdAt) }}</small>
                  </button>

                  <p v-if="noticeRows.length === 0" class="muted">暂无通知</p>
                </div>

                <div class="inline notice-ops">
                  <button
                    type="button"
                    class="secondary"
                    @click="markAllNoticeRead"
                    :disabled="noticeActionLoading"
                  >
                    {{ noticeActionLoading ? '处理中...' : '批量已读' }}
                  </button>
                  <button
                    type="button"
                    class="danger"
                    @click="deleteReadNotices"
                    :disabled="noticeActionLoading"
                  >
                    {{ noticeActionLoading ? '处理中...' : '删除已读' }}
                  </button>
                </div>

                <button type="button" class="ghost-link notice-more" @click="openNoticeCenter">查看更多</button>
              </section>
            </div>

            <div class="user-menu" ref="userMenuRef">
              <button
                type="button"
                class="user-trigger"
                @click="toggleUserMenu"
                :aria-expanded="userMenuOpen"
              >
                {{ displayName }} ({{ auth.state.user.username }})
                <span class="caret">{{ userMenuOpen ? '▴' : '▾' }}</span>
              </button>

              <div v-if="userMenuOpen" class="user-dropdown">
                <button type="button" class="dropdown-item" @click="openProfileModal">个人信息</button>
                <button type="button" class="dropdown-item" @click="openPasswordModal">修改密码</button>
                <button type="button" class="dropdown-item danger-text" @click="onLogout">退出登录</button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main class="page-wrap" :class="{ compact: !showLayout }">
        <RouterView />
      </main>

      <div v-if="noticeCenterOpen" class="modal-mask" @click.self="closeNoticeCenter">
        <section class="modal-card notice-center-modal">
          <div class="modal-head">
            <h3>通知中心</h3>
            <button type="button" class="icon-ghost" @click="closeNoticeCenter">关闭</button>
          </div>

          <div v-if="noticeCenterError" class="alert alert-error">{{ noticeCenterError }}</div>
          <div v-if="noticeCenterSuccess" class="alert alert-success">{{ noticeCenterSuccess }}</div>

          <form class="inline notice-center-toolbar" @submit.prevent="loadNoticeCenterRows">
            <label style="margin-bottom: 0;">仅看未读</label>
            <select v-model="noticeCenterUnreadOnly" style="width: 120px; margin-bottom: 0;">
              <option :value="false">否</option>
              <option :value="true">是</option>
            </select>

            <button type="submit" :disabled="noticeCenterLoading || noticeCenterBatchLoading">
              {{ noticeCenterLoading ? '加载中...' : '刷新' }}
            </button>

            <button
              type="button"
              class="secondary"
              @click="markAllNoticeReadInCenter"
              :disabled="noticeCenterLoading || noticeCenterBatchLoading"
            >
              {{ noticeCenterBatchLoading ? '处理中...' : '批量已读' }}
            </button>

            <button
              type="button"
              class="danger"
              @click="deleteReadNoticesInCenter"
              :disabled="noticeCenterLoading || noticeCenterBatchLoading"
            >
              {{ noticeCenterBatchLoading ? '处理中...' : '删除已读' }}
            </button>
          </form>

          <p class="muted">未读数量：{{ noticeUnreadCount }}</p>

          <div v-if="noticeCenterLoading" class="muted">正在加载通知...</div>

          <div v-else class="table-wrap notice-center-table-wrap">
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
                <tr v-for="row in noticeCenterRows" :key="row.id">
                  <td>
                    <span :class="row.readFlag ? 'badge badge-approved' : 'badge badge-pending'">
                      {{ row.readFlag ? '已读' : '未读' }}
                    </span>
                  </td>
                  <td>{{ row.title }}</td>
                  <td>{{ row.content }}</td>
                  <td>{{ formatNoticeTime(row.createdAt) }}</td>
                  <td class="inline">
                    <button
                      v-if="!row.readFlag"
                      type="button"
                      @click="markNoticeReadInCenter(row.id)"
                      :disabled="noticeCenterActionLoadingId === row.id"
                    >
                      {{ noticeCenterActionLoadingId === row.id ? '处理中...' : '标已读' }}
                    </button>
                    <button
                      type="button"
                      class="secondary"
                      @click="viewNoticeFromCenter(row)"
                      :disabled="noticeCenterActionLoadingId === row.id"
                    >查看</button>
                    <button
                      type="button"
                      class="danger"
                      @click="deleteNoticeInCenter(row.id)"
                      :disabled="noticeCenterActionLoadingId === row.id"
                    >删除</button>
                  </td>
                </tr>
                <tr v-if="noticeCenterRows.length === 0">
                  <td colspan="5" class="muted">暂无通知</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div v-if="profileModalOpen" class="modal-mask" @click.self="closeProfileModal">
        <section class="modal-card profile-modal">
          <div class="modal-head">
            <h3>个人信息</h3>
            <button type="button" class="icon-ghost" @click="closeProfileModal">关闭</button>
          </div>

          <div class="profile-list">
            <div class="profile-item">
              <span class="muted">姓名</span>
              <strong>{{ auth.state.user?.name || '-' }}</strong>
            </div>
            <div class="profile-item">
              <span class="muted">账号</span>
              <strong>{{ auth.state.user?.username || '-' }}</strong>
            </div>
            <div class="profile-item">
              <span class="muted">角色</span>
              <strong>{{ auth.state.user?.role || '-' }}</strong>
            </div>
            <div class="profile-item" v-if="auth.state.user?.className">
              <span class="muted">班级</span>
              <strong>{{ auth.state.user.className }}</strong>
            </div>
            <div class="profile-item" v-if="auth.state.user?.department">
              <span class="muted">部门</span>
              <strong>{{ auth.state.user.department }}</strong>
            </div>
          </div>
        </section>
      </div>

      <div v-if="passwordModalOpen" class="modal-mask" @click.self="closePasswordModal">
        <section class="modal-card password-modal">
          <div class="modal-head">
            <h3>修改密码</h3>
            <button type="button" class="icon-ghost" @click="closePasswordModal">关闭</button>
          </div>

          <p class="muted">修改后将立即生效。</p>
          <div v-if="passwordError" class="alert alert-error">{{ passwordError }}</div>
          <div v-if="passwordSuccess" class="alert alert-success">{{ passwordSuccess }}</div>

          <form @submit.prevent="onChangePassword">
            <label>旧密码</label>
            <input v-model="oldPassword" type="password" required />

            <label>新密码</label>
            <input v-model="newPassword" type="password" minlength="6" required />

            <label>确认新密码</label>
            <input v-model="confirmPassword" type="password" minlength="6" required />

            <div class="inline">
              <button type="submit" :disabled="passwordLoading">
                {{ passwordLoading ? '更新中...' : '确认修改' }}
              </button>
              <button type="button" class="secondary" @click="closePasswordModal">取消</button>
            </div>
          </form>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router';
import { useAuth } from './lib/auth';
import { request } from './lib/http';
import { ROLE } from './lib/roles';

const route = useRoute();
const router = useRouter();
const auth = useAuth();

const showLayout = computed(() => route.path !== '/login');
const noticeMenuRef = ref(null);
const noticeMenuOpen = ref(false);
const noticeRows = ref([]);
const noticeUnreadCount = ref(0);
const noticeLoading = ref(false);
const noticeActionLoading = ref(false);

const noticeCenterOpen = ref(false);
const noticeCenterRows = ref([]);
const noticeCenterUnreadOnly = ref(false);
const noticeCenterLoading = ref(false);
const noticeCenterBatchLoading = ref(false);
const noticeCenterActionLoadingId = ref(null);
const noticeCenterError = ref('');
const noticeCenterSuccess = ref('');

const userMenuRef = ref(null);
const userMenuOpen = ref(false);

const profileModalOpen = ref(false);
const passwordModalOpen = ref(false);
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const passwordSuccess = ref('');
const passwordLoading = ref(false);

let noticeRefreshTimer = null;

const roleMenus = {
  [ROLE.ADMIN]: [
    { label: '学生管理', to: '/admin/students' },
    { label: '老师管理', to: '/admin/teachers' },
    { label: '组织管理', to: '/admin/org' },
    { label: '账号管理', to: '/admin/accounts' },
    { label: '规则配置', to: '/admin/rules' },
    { label: '全局成绩', to: '/admin/scores' },
    { label: '导入批次', to: '/admin/import-batches' },
    { label: '系统日志', to: '/admin/logs' }
  ],
  [ROLE.TEACHER_PE]: [
    { label: '体育导入', to: '/teacher/pe/import' },
    { label: '体育录入', to: '/teacher/pe/edit' },
    { label: '体育查询/修改', to: '/teacher/pe/list' }
  ],
  [ROLE.TEACHER_STUDY]: [
    { label: '智育导入', to: '/teacher/study/import' },
    { label: '智育录入', to: '/teacher/study/edit' },
    { label: '智育查询/修改', to: '/teacher/study/list' }
  ],
  [ROLE.COUNSELOR]: [
    { label: '德育审核', to: '/counselor/review/moral' },
    { label: '技能审核', to: '/counselor/review/skill' },
    { label: '班级汇总', to: '/counselor/class-summary' }
  ],
  [ROLE.STUDENT]: [
    { label: '德育申报', to: '/student/declare/moral' },
    { label: '技能申报', to: '/student/declare/skill' },
    { label: '我的申报', to: '/student/declare/history' },
    { label: '我的成绩', to: '/student/my-score' }
  ]
};

const menuItems = computed(() => {
  const role = auth.state.user?.role;
  return roleMenus[role] || [];
});

const displayName = computed(() => {
  return auth.state.user?.name || '用户';
});

const noticeBadgeText = computed(() => {
  return noticeUnreadCount.value > 99 ? '99+' : String(noticeUnreadCount.value);
});

watch(() => route.fullPath, () => {
  noticeMenuOpen.value = false;
  noticeCenterOpen.value = false;
  userMenuOpen.value = false;
  profileModalOpen.value = false;
  passwordModalOpen.value = false;
});

onMounted(() => {
  document.addEventListener('click', onDocumentClick);
  window.addEventListener('notifications-updated', onNotificationsUpdated);
  void loadNoticePreview(true);
  noticeRefreshTimer = window.setInterval(() => {
    void loadNoticePreview(true);
  }, 30000);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick);
  window.removeEventListener('notifications-updated', onNotificationsUpdated);
  if (noticeRefreshTimer) {
    window.clearInterval(noticeRefreshTimer);
  }
});

function onDocumentClick(event) {
  if (noticeMenuRef.value && !noticeMenuRef.value.contains(event.target)) {
    noticeMenuOpen.value = false;
  }
  if (userMenuRef.value && !userMenuRef.value.contains(event.target)) {
    userMenuOpen.value = false;
  }
}

function onNotificationsUpdated() {
  void loadNoticePreview(true);
  if (noticeCenterOpen.value) {
    void loadNoticeCenterRows();
  }
}

function toggleNoticeMenu() {
  userMenuOpen.value = false;
  noticeMenuOpen.value = !noticeMenuOpen.value;
  if (noticeMenuOpen.value) {
    void loadNoticePreview();
  }
}

function toggleUserMenu() {
  noticeMenuOpen.value = false;
  userMenuOpen.value = !userMenuOpen.value;
}

async function loadNoticePreview(silent = false) {
  if (!auth.state.user) {
    noticeRows.value = [];
    noticeUnreadCount.value = 0;
    return;
  }
  try {
    if (!silent) {
      noticeLoading.value = true;
    }
    const res = await request('/api/notifications?limit=6');
    noticeRows.value = res.data || [];
    noticeUnreadCount.value = res.unreadCount || 0;
  } catch {
    if (!silent) {
      noticeRows.value = [];
    }
  } finally {
    if (!silent) {
      noticeLoading.value = false;
    }
  }
}

async function openNoticeDetail(row) {
  if (!row) {
    return;
  }
  noticeMenuOpen.value = false;

  try {
    if (!row.readFlag) {
      await request(`/api/notifications/${row.id}/read`, { method: 'POST' });
      window.dispatchEvent(new Event('notifications-updated'));
    }
  } catch {
    // ignore read failure and continue navigation
  }

  const target = row.linkUrl || '/notifications';
  if (route.path !== target) {
    await router.push(target);
  }
}

function setNoticeCenterMessage(type, text) {
  if (type === 'error') {
    noticeCenterError.value = text;
    noticeCenterSuccess.value = '';
  } else {
    noticeCenterSuccess.value = text;
    noticeCenterError.value = '';
  }
}

async function openNoticeCenter() {
  noticeMenuOpen.value = false;
  userMenuOpen.value = false;
  noticeCenterOpen.value = true;
  noticeCenterError.value = '';
  noticeCenterSuccess.value = '';
  await loadNoticeCenterRows();
}

function closeNoticeCenter() {
  noticeCenterOpen.value = false;
  noticeCenterError.value = '';
  noticeCenterSuccess.value = '';
}

async function loadNoticeCenterRows() {
  try {
    noticeCenterLoading.value = true;
    noticeCenterError.value = '';
    const query = noticeCenterUnreadOnly.value ? '?unreadOnly=true' : '';
    const res = await request(`/api/notifications${query}`);
    noticeCenterRows.value = res.data || [];
    noticeUnreadCount.value = res.unreadCount || 0;
  } catch (e) {
    setNoticeCenterMessage('error', e?.message || '加载通知失败');
  } finally {
    noticeCenterLoading.value = false;
  }
}

async function markNoticeReadInCenter(id) {
  try {
    noticeCenterActionLoadingId.value = id;
    const res = await request(`/api/notifications/${id}/read`, { method: 'POST' });
    setNoticeCenterMessage('success', res?.message || '操作成功');
    window.dispatchEvent(new Event('notifications-updated'));
  } catch (e) {
    setNoticeCenterMessage('error', e?.message || '操作失败');
  } finally {
    noticeCenterActionLoadingId.value = null;
  }
}

async function deleteNoticeInCenter(id) {
  if (!window.confirm('确认删除这条通知吗？')) {
    return;
  }
  try {
    noticeCenterActionLoadingId.value = id;
    const res = await request(`/api/notifications/${id}`, { method: 'DELETE' });
    setNoticeCenterMessage('success', res?.message || '删除成功');
    window.dispatchEvent(new Event('notifications-updated'));
  } catch (e) {
    setNoticeCenterMessage('error', e?.message || '删除失败');
  } finally {
    noticeCenterActionLoadingId.value = null;
  }
}

async function markAllNoticeReadInCenter() {
  if (!window.confirm('确认将当前账号所有通知标记为已读吗？')) {
    return;
  }
  try {
    noticeCenterBatchLoading.value = true;
    const res = await request('/api/notifications/read-all', { method: 'POST' });
    setNoticeCenterMessage('success', res?.message || '操作成功');
    window.dispatchEvent(new Event('notifications-updated'));
  } catch (e) {
    setNoticeCenterMessage('error', e?.message || '操作失败');
  } finally {
    noticeCenterBatchLoading.value = false;
  }
}

async function deleteReadNoticesInCenter() {
  if (!window.confirm('确认删除所有已读通知吗？')) {
    return;
  }
  try {
    noticeCenterBatchLoading.value = true;
    const res = await request('/api/notifications/delete-read', { method: 'POST' });
    setNoticeCenterMessage('success', res?.message || '操作成功');
    window.dispatchEvent(new Event('notifications-updated'));
  } catch (e) {
    setNoticeCenterMessage('error', e?.message || '操作失败');
  } finally {
    noticeCenterBatchLoading.value = false;
  }
}

async function viewNoticeFromCenter(row) {
  if (!row) {
    return;
  }
  if (!row.readFlag) {
    await markNoticeReadInCenter(row.id);
  }
  closeNoticeCenter();
  const target = row.linkUrl || '/dashboard';
  if (route.path !== target) {
    await router.push(target);
  }
}

async function markAllNoticeRead() {
  if (!window.confirm('确认将当前账号所有通知标记为已读吗？')) {
    return;
  }
  try {
    noticeActionLoading.value = true;
    const res = await request('/api/notifications/read-all', { method: 'POST' });
    window.dispatchEvent(new Event('notifications-updated'));
    window.alert(res?.message || '操作成功');
  } catch (e) {
    window.alert(e?.message || '操作失败');
  } finally {
    noticeActionLoading.value = false;
  }
}

async function deleteReadNotices() {
  if (!window.confirm('确认删除所有已读通知吗？')) {
    return;
  }
  try {
    noticeActionLoading.value = true;
    const res = await request('/api/notifications/delete-read', { method: 'POST' });
    window.dispatchEvent(new Event('notifications-updated'));
    window.alert(res?.message || '操作成功');
  } catch (e) {
    window.alert(e?.message || '操作失败');
  } finally {
    noticeActionLoading.value = false;
  }
}

function formatNoticeTime(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').slice(0, 19);
}

function openProfileModal() {
  userMenuOpen.value = false;
  noticeMenuOpen.value = false;
  closeNoticeCenter();
  closePasswordModal();
  profileModalOpen.value = true;
}

function closeProfileModal() {
  profileModalOpen.value = false;
}

function openPasswordModal() {
  userMenuOpen.value = false;
  noticeMenuOpen.value = false;
  closeNoticeCenter();
  closeProfileModal();
  passwordModalOpen.value = true;
  passwordError.value = '';
  passwordSuccess.value = '';
  oldPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
}

function closePasswordModal() {
  passwordModalOpen.value = false;
  passwordError.value = '';
  passwordSuccess.value = '';
  oldPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
}

async function onChangePassword() {
  passwordError.value = '';
  passwordSuccess.value = '';

  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = '两次输入的新密码不一致';
    return;
  }
  if (newPassword.value.length < 6) {
    passwordError.value = '新密码至少 6 位';
    return;
  }
  if (oldPassword.value === newPassword.value) {
    passwordError.value = '新密码不能与旧密码相同';
    return;
  }

  passwordLoading.value = true;
  try {
    const res = await auth.changePassword(oldPassword.value, newPassword.value);
    passwordSuccess.value = res.message || '密码已更新';
    oldPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';
  } catch (e) {
    passwordError.value = e.message || '更新失败';
  } finally {
    passwordLoading.value = false;
  }
}

async function onLogout() {
  noticeMenuOpen.value = false;
  userMenuOpen.value = false;
  closeNoticeCenter();
  closeProfileModal();
  closePasswordModal();
  noticeRows.value = [];
  noticeUnreadCount.value = 0;
  noticeCenterRows.value = [];
  await auth.logout();
  await router.push('/login');
}
</script>
