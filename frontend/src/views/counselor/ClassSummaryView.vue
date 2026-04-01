<template>
  <div>
    <div class="card">
      <h2>班级综测汇总</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>
      <p v-if="scopedClass" class="muted">当前账号仅可查看班级：{{ scopedClass }}</p>

      <form class="inline" @submit.prevent="loadRows">
        <input v-model.trim="className" :readonly="!!scopedClass" placeholder="班级" style="width: 220px; margin-bottom: 0;" />
        <input v-model.trim="term" placeholder="学期，如 2026-1" style="width: 180px; margin-bottom: 0;" />
        <button type="submit">查询</button>
        <button type="button" class="secondary" @click="exportCsv">导出CSV</button>
      </form>

      <form class="inline" style="margin-top: 10px;" @submit.prevent="recalc">
        <input v-model.trim="className" :readonly="!!scopedClass" placeholder="班级" style="width: 220px; margin-bottom: 0;" required />
        <input v-model.trim="term" placeholder="学期" style="width: 180px; margin-bottom: 0;" required />
        <button type="submit">一键重算本班综测</button>
      </form>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>排名</th>
            <th>学号</th>
            <th>姓名</th>
            <th>智育</th>
            <th>体育</th>
            <th>德育</th>
            <th>技能</th>
            <th>总分</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.studentNo">
            <td>{{ r.rankNo }}</td>
            <td>{{ r.studentNo }}</td>
            <td>{{ r.studentName }}</td>
            <td>{{ r.studyScore }}</td>
            <td>{{ r.peScore }}</td>
            <td>{{ r.moralScore }}</td>
            <td>{{ r.skillScore }}</td>
            <td>{{ r.totalScore }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="8" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../../lib/http';

const className = ref('');
const term = ref('2026-1');
const scopedClass = ref('');
const rows = ref([]);
const error = ref('');
const success = ref('');

async function loadRows() {
  error.value = '';
  success.value = '';
  try {
    const params = new URLSearchParams();
    if (className.value) params.append('className', className.value);
    if (term.value) params.append('term', term.value);

    const res = await request(`/api/counselor/class-summary?${params.toString()}`);
    rows.value = res.data || [];
    scopedClass.value = res.scopedClass || '';
    if (scopedClass.value) {
      className.value = scopedClass.value;
    } else {
      className.value = res.className || className.value;
    }
    term.value = res.term || term.value;
  } catch (e) {
    error.value = e.message || '加载失败';
  }
}

async function recalc() {
  error.value = '';
  success.value = '';
  try {
    const res = await request('/api/counselor/class-summary/recalc', {
      method: 'POST',
      data: { className: className.value, term: term.value }
    });
    success.value = res.message || '重算完成';
    loadRows();
  } catch (e) {
    error.value = e.message || '重算失败';
  }
}

function exportCsv() {
  error.value = '';
  const params = new URLSearchParams();
  if (className.value) params.append('className', className.value);
  if (term.value) params.append('term', term.value);
  window.open(`/api/counselor/class-summary/export?${params.toString()}`, '_blank');
}

onMounted(loadRows);
</script>
