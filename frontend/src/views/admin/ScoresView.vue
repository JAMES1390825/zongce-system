<template>
  <div>
    <div class="card">
      <h2>全局成绩查询</h2>
      <p class="muted">至少输入班级或学期之一</p>

      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <form class="inline" @submit.prevent="loadRows">
        <input v-model.trim="className" placeholder="班级，如 软件221" style="width: 220px; margin-bottom: 0;" />
        <input v-model.trim="term" placeholder="学期，如 2026-1" style="width: 180px; margin-bottom: 0;" />
        <button type="submit">查询</button>
        <button type="button" class="secondary" @click="exportCsv">导出CSV</button>
      </form>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>学期</th>
            <th>班级</th>
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
          <tr v-for="r in rows" :key="`${r.term}-${r.className}-${r.studentNo}`">
            <td>{{ r.term }}</td>
            <td>{{ r.className }}</td>
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
            <td colspan="10" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { request } from '../../lib/http';

const className = ref('');
const term = ref('');
const rows = ref([]);
const error = ref('');

async function loadRows() {
  error.value = '';
  try {
    const params = new URLSearchParams();
    if (className.value) params.append('className', className.value);
    if (term.value) params.append('term', term.value);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/api/admin/scores${query}`);
    rows.value = res.data || [];
  } catch (e) {
    error.value = e.message || '查询失败';
  }
}

function exportCsv() {
  error.value = '';
  const params = new URLSearchParams();
  if (className.value) params.append('className', className.value);
  if (term.value) params.append('term', term.value);
  if (!params.toString()) {
    error.value = '请至少输入班级或学期后再导出';
    return;
  }
  window.open(`/api/admin/scores/export?${params.toString()}`, '_blank');
}
</script>
