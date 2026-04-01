<template>
  <div>
    <div class="card">
      <h2>导入批次</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <form class="inline" @submit.prevent="loadBatches">
        <input
          v-model.trim="importType"
          placeholder="导入类型，如 STUDENT/PE_SCORE/STUDY_SCORE"
          style="width: 320px; margin-bottom: 0;"
        />
        <input
          v-model.trim="operator"
          placeholder="操作人，如 admin"
          style="width: 180px; margin-bottom: 0;"
        />
        <button type="submit">查询</button>
        <button type="button" class="secondary" @click="clearFilter">清空</button>
      </form>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>批次号</th>
            <th>类型</th>
            <th>状态</th>
            <th>总数</th>
            <th>成功</th>
            <th>失败</th>
            <th>操作人</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in batches" :key="row.batchNo">
            <td>{{ row.batchNo }}</td>
            <td>{{ row.importType }}</td>
            <td>{{ row.status }}</td>
            <td>{{ row.totalCount }}</td>
            <td>{{ row.successCount }}</td>
            <td>{{ row.failedCount }}</td>
            <td>{{ row.operator || '-' }}</td>
            <td>{{ row.createdAt || '-' }}</td>
            <td class="inline">
              <button class="secondary" @click="loadErrors(row.batchNo)">查看失败详情</button>
            </td>
          </tr>
          <tr v-if="batches.length === 0">
            <td colspan="9" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card" v-if="currentBatchNo">
      <h3>失败详情：{{ currentBatchNo }}</h3>
      <table>
        <thead>
          <tr>
            <th>行号</th>
            <th>错误原因</th>
            <th>原始内容</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in errors" :key="row.id">
            <td>{{ row.lineNo }}</td>
            <td>{{ row.errorMessage }}</td>
            <td>{{ row.rawContent || '-' }}</td>
            <td>{{ row.createdAt || '-' }}</td>
          </tr>
          <tr v-if="errors.length === 0">
            <td colspan="4" class="muted">该批次无失败明细</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../../lib/http';

const importType = ref('');
const operator = ref('');
const batches = ref([]);
const errors = ref([]);
const currentBatchNo = ref('');
const error = ref('');

async function loadBatches() {
  try {
    error.value = '';
    const params = new URLSearchParams();
    if (importType.value) params.append('importType', importType.value);
    if (operator.value) params.append('operator', operator.value);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/api/admin/import-batches${query}`);
    batches.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载导入批次失败';
  }
}

async function loadErrors(batchNo) {
  try {
    error.value = '';
    const res = await request(`/api/admin/import-batches/${encodeURIComponent(batchNo)}/errors`);
    currentBatchNo.value = batchNo;
    errors.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载失败明细失败';
  }
}

function clearFilter() {
  importType.value = '';
  operator.value = '';
  loadBatches();
}

onMounted(loadBatches);
</script>
