<template>
  <div class="card">
    <h2>我的申报记录</h2>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <table>
      <thead>
        <tr>
          <th>时间</th>
          <th>学期</th>
          <th>类型</th>
          <th>项目</th>
          <th>分值</th>
          <th>状态</th>
          <th>批注</th>
          <th>附件</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r.id">
          <td>{{ formatDate(r.createdAt) }}</td>
          <td>{{ r.term }}</td>
          <td>{{ formatType(r.type) }}</td>
          <td>{{ r.itemName }}</td>
          <td>{{ r.points }}</td>
          <td>
            <span v-if="r.status === 'PENDING'" class="badge badge-pending">待审核</span>
            <span v-else-if="r.status === 'APPROVED'" class="badge badge-approved">已通过</span>
            <span v-else class="badge badge-rejected">已驳回</span>
          </td>
          <td>{{ r.reviewComment }}</td>
          <td>
            <a v-if="r.attachmentPath" :href="r.attachmentPath" target="_blank">查看</a>
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td colspan="8" class="muted">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../../lib/http';

const rows = ref([]);
const error = ref('');

function formatDate(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

function formatType(type) {
  if (type === 'MORAL') return '德育';
  if (type === 'SKILL') return '技能';
  return type || '-';
}

async function loadRows() {
  error.value = '';
  try {
    const res = await request('/api/student/declarations');
    rows.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载失败';
  }
}

onMounted(loadRows);
</script>
