<template>
  <div>
    <div class="card">
      <h2>{{ pageTitle }}</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <div class="inline" style="margin-bottom: 10px;">
        <RouterLink to="/counselor/review/moral">德育审核</RouterLink>
        <RouterLink to="/counselor/review/skill">技能审核</RouterLink>
        <RouterLink to="/counselor/reviews">全部类型</RouterLink>
      </div>

      <form class="inline" @submit.prevent="loadRows">
        <label style="margin-bottom: 0;">状态</label>
        <select v-model="status" style="width: 180px; margin-bottom: 0;">
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <button type="submit" :disabled="loading">{{ loading ? '加载中...' : '筛选' }}</button>
      </form>
    </div>

    <div class="card">
      <div v-if="loading" class="muted">正在加载审核数据...</div>

      <div v-else class="table-wrap">
        <table>
        <thead>
          <tr>
            <th>学号</th>
            <th>姓名</th>
            <th>班级</th>
            <th>学期</th>
            <th>类型</th>
            <th>项目</th>
            <th>分值</th>
            <th>说明</th>
            <th>附件</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in pagedRows" :key="r.id">
            <td>{{ r.studentNo }}</td>
            <td>{{ r.studentName }}</td>
            <td>{{ r.className }}</td>
            <td>{{ r.term }}</td>
            <td>{{ formatType(r.type) }}</td>
            <td>{{ r.itemName }}</td>
            <td>{{ r.points }}</td>
            <td>{{ r.description }}</td>
            <td>
              <a v-if="r.attachmentPath" :href="r.attachmentPath" target="_blank">查看</a>
            </td>
            <td>
              <div v-if="r.status === 'PENDING'" class="inline">
                <button
                  @click="approve(r.id)"
                  :disabled="actionLoadingId === r.id"
                >
                  {{ actionLoadingId === r.id ? '处理中...' : '通过' }}
                </button>
                <button
                  class="danger"
                  @click="openRejectModal(r.id)"
                  :disabled="actionLoadingId === r.id"
                >
                  驳回
                </button>
              </div>
              <span v-else>{{ r.reviewComment }}</span>
            </td>
          </tr>
          <tr v-if="pagedRows.length === 0">
            <td colspan="10" class="muted">暂无数据</td>
          </tr>
        </tbody>
        </table>
      </div>

      <div class="inline" v-if="rows.length > pageSize" style="margin-top: 12px; justify-content: space-between;">
        <span class="muted">共 {{ rows.length }} 条，第 {{ currentPage }} / {{ totalPages }} 页</span>
        <div class="inline">
          <button class="secondary" @click="prevPage" :disabled="currentPage <= 1">上一页</button>
          <button class="secondary" @click="nextPage" :disabled="currentPage >= totalPages">下一页</button>
        </div>
      </div>
    </div>

    <div v-if="rejectModalVisible" class="modal-mask" @click="closeRejectModal">
      <div class="modal-card" @click.stop>
        <h3 style="margin-top: 0;">驳回申报</h3>
        <p class="muted">请填写驳回原因，学生端会看到这条说明。</p>
        <textarea v-model.trim="rejectComment" placeholder="例如：证明材料不完整，请补充获奖证书图片。" />
        <div class="inline" style="justify-content: flex-end;">
          <button class="secondary" @click="closeRejectModal">取消</button>
          <button class="danger" @click="confirmReject" :disabled="actionLoadingId === rejectTargetId || !rejectComment">
            {{ actionLoadingId === rejectTargetId ? '提交中...' : '确认驳回' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { request } from '../../lib/http';

const route = useRoute();

const type = computed(() => route.meta.type || '');
const pageTitle = computed(() => {
  if (type.value === 'MORAL') return '德育审核';
  if (type.value === 'SKILL') return '技能审核';
  return '申报审核';
});

const status = ref('PENDING');
const rows = ref([]);
const loading = ref(false);
const actionLoadingId = ref(null);
const error = ref('');
const success = ref('');
const pageSize = 10;
const currentPage = ref(1);

const rejectModalVisible = ref(false);
const rejectTargetId = ref(null);
const rejectComment = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return rows.value.slice(start, start + pageSize);
});

function resetPage() {
  currentPage.value = 1;
}

function formatType(typeValue) {
  if (typeValue === 'MORAL') return '德育';
  if (typeValue === 'SKILL') return '技能';
  return typeValue || '-';
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1;
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value -= 1;
  }
}

async function loadRows() {
  error.value = '';
  loading.value = true;
  try {
    const params = new URLSearchParams();
    params.append('status', status.value);
    if (type.value) params.append('type', type.value);
    const res = await request(`/api/counselor/reviews?${params.toString()}`);
    rows.value = res.data || [];
    resetPage();
  } catch (e) {
    error.value = e.message || '加载失败';
  } finally {
    loading.value = false;
  }
}

async function approve(id) {
  if (!window.confirm('确认通过该申报吗？')) {
    return;
  }
  try {
    error.value = '';
    actionLoadingId.value = id;
    const res = await request(`/api/counselor/reviews/${id}/approve`, { method: 'POST' });
    success.value = res.message || '已通过';
    await loadRows();
  } catch (e) {
    error.value = e.message || '操作失败';
  } finally {
    actionLoadingId.value = null;
  }
}

function openRejectModal(id) {
  rejectTargetId.value = id;
  rejectComment.value = '材料不完整';
  rejectModalVisible.value = true;
}

function closeRejectModal() {
  rejectModalVisible.value = false;
  rejectTargetId.value = null;
  rejectComment.value = '';
}

async function confirmReject() {
  if (!rejectTargetId.value || !rejectComment.value) {
    return;
  }
  try {
    error.value = '';
    actionLoadingId.value = rejectTargetId.value;
    const res = await request(`/api/counselor/reviews/${rejectTargetId.value}/reject`, {
      method: 'POST',
      data: { comment: rejectComment.value }
    });
    success.value = res.message || '已驳回';
    closeRejectModal();
    await loadRows();
  } catch (e) {
    error.value = e.message || '操作失败';
  } finally {
    actionLoadingId.value = null;
  }
}

onMounted(loadRows);

watch(
  () => route.fullPath,
  () => {
    loadRows();
  }
);
</script>
