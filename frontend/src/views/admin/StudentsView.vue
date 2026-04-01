<template>
  <div>
    <div class="card">
      <h2>学生管理</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <form class="inline" @submit.prevent="loadRows">
        <input v-model.trim="className" placeholder="按班级筛选，如 软件221" style="width: 220px; margin-bottom: 0;" />
        <button type="submit">筛选</button>
        <button type="button" class="secondary" @click="clearFilter">清空</button>
      </form>
    </div>

    <div class="card">
      <h3>新增学生</h3>
      <form @submit.prevent="createStudent">
        <div class="grid">
          <div>
            <label>账号</label>
            <input v-model.trim="createForm.username" required />
          </div>
          <div>
            <label>姓名</label>
            <input v-model.trim="createForm.name" required />
          </div>
          <div>
            <label>班级</label>
            <input v-model.trim="createForm.className" required />
          </div>
        </div>
        <button type="submit">创建学生</button>
      </form>
    </div>

    <div class="card">
      <h3>批量导入学生（CSV）</h3>
      <p class="muted">字段顺序：学号(或账号), 姓名, 班级。首行可带表头。</p>
      <form class="inline" @submit.prevent="importStudents">
        <input
          ref="importInput"
          type="file"
          accept=".csv,text/csv"
          style="max-width: 360px; margin-bottom: 0;"
          @change="onSelectImportFile"
        />
        <button type="submit">上传并导入</button>
        <a class="ghost-link" href="/api/templates/students-import.csv" download>下载模板</a>
      </form>
    </div>

    <div class="card">
      <h3>学生列表</h3>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>账号</th>
            <th>姓名</th>
            <th>班级</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td><input v-model.trim="row.username" style="width: 120px; margin-bottom: 0;" /></td>
            <td><input v-model.trim="row.name" style="width: 120px; margin-bottom: 0;" /></td>
            <td><input v-model.trim="row.className" style="width: 140px; margin-bottom: 0;" /></td>
            <td>
              <select v-model="row.enabled" style="width: 90px; margin-bottom: 0;">
                <option :value="true">启用</option>
                <option :value="false">禁用</option>
              </select>
            </td>
            <td class="inline">
              <button @click="updateRow(row)">保存</button>
              <button class="secondary" @click="resetPassword(row.id)">重置密码</button>
              <button class="danger" @click="deleteRow(row.id)">删除</button>
            </td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="6" class="muted">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../lib/http';

const className = ref('');
const rows = ref([]);
const error = ref('');
const success = ref('');

const createForm = reactive({
  username: '',
  name: '',
  className: ''
});
const importFile = ref(null);
const importInput = ref(null);

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
    const query = className.value ? `?className=${encodeURIComponent(className.value)}` : '';
    const res = await request(`/api/admin/students${query}`);
    rows.value = (res.data || []).map((r) => ({ ...r }));
  } catch (e) {
    setMessage('error', e.message || '加载失败');
  }
}

function clearFilter() {
  className.value = '';
  loadRows();
}

async function createStudent() {
  try {
    const res = await request('/api/admin/students', {
      method: 'POST',
      data: createForm
    });
    setMessage('success', res.message || '创建成功');
    createForm.username = '';
    createForm.name = '';
    createForm.className = '';
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '创建失败');
  }
}

function onSelectImportFile(event) {
  importFile.value = event.target?.files?.[0] || null;
}

async function importStudents() {
  if (!importFile.value) {
    setMessage('error', '请先选择 CSV 文件');
    return;
  }
  try {
    const form = new FormData();
    form.append('file', importFile.value);
    const res = await request('/api/admin/students/import', {
      method: 'POST',
      data: form,
      isForm: true
    });
    const summary = res.data || {};
    setMessage(
      'success',
      `导入完成：批次 ${summary.batchNo || '-'}，新增 ${summary.created ?? 0}，更新 ${summary.updated ?? 0}，失败 ${summary.failed ?? 0}`
    );
    importFile.value = null;
    if (importInput.value) {
      importInput.value.value = '';
    }
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '导入失败');
  }
}

async function updateRow(row) {
  try {
    const res = await request(`/api/admin/students/${row.id}`, {
      method: 'PUT',
      data: {
        username: row.username,
        name: row.name,
        className: row.className,
        enabled: row.enabled
      }
    });
    setMessage('success', res.message || '保存成功');
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '保存失败');
  }
}

async function resetPassword(id) {
  try {
    const res = await request(`/api/admin/students/${id}/reset-password`, { method: 'POST' });
    setMessage('success', res.message || '重置成功');
  } catch (e) {
    setMessage('error', e.message || '重置失败');
  }
}

async function deleteRow(id) {
  if (!window.confirm('确认删除该学生账号吗？')) {
    return;
  }
  try {
    const res = await request(`/api/admin/students/${id}`, { method: 'DELETE' });
    setMessage('success', res.message || '删除成功');
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '删除失败');
  }
}

onMounted(loadRows);
</script>
