<template>
  <div>
    <div class="card">
      <h2>班级/部门管理</h2>
      <p class="muted">维护系统基础组织数据，供账号与业务使用。</p>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>
    </div>

    <div class="card">
      <h3>新增班级</h3>
      <form @submit.prevent="createClass">
        <div class="grid">
          <div>
            <label>班级名称</label>
            <input v-model.trim="newClass.className" required placeholder="如 软件221" />
          </div>
          <div>
            <label>所属专业</label>
            <input v-model.trim="newClass.majorName" placeholder="如 软件工程" />
          </div>
        </div>
        <button type="submit">新增班级</button>
      </form>
    </div>

    <div class="card">
      <h3>班级列表</h3>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>班级</th>
            <th>专业</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in classes" :key="row.id">
            <td>{{ row.id }}</td>
            <td><input v-model.trim="row.className" style="width: 140px; margin-bottom: 0;" /></td>
            <td><input v-model.trim="row.majorName" style="width: 160px; margin-bottom: 0;" /></td>
            <td>
              <select v-model="row.enabled" style="width: 90px; margin-bottom: 0;">
                <option :value="true">启用</option>
                <option :value="false">禁用</option>
              </select>
            </td>
            <td class="inline">
              <button @click="updateClass(row)">保存</button>
              <button class="danger" @click="deleteClass(row.id)">删除</button>
            </td>
          </tr>
          <tr v-if="classes.length === 0">
            <td colspan="5" class="muted">暂无班级数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3>新增部门</h3>
      <form @submit.prevent="createDepartment">
        <div class="grid">
          <div>
            <label>部门名称</label>
            <input v-model.trim="newDepartment.departmentName" required placeholder="如 学习部" />
          </div>
        </div>
        <button type="submit">新增部门</button>
      </form>
    </div>

    <div class="card">
      <h3>部门列表</h3>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>部门</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in departments" :key="row.id">
            <td>{{ row.id }}</td>
            <td><input v-model.trim="row.departmentName" style="width: 180px; margin-bottom: 0;" /></td>
            <td>
              <select v-model="row.enabled" style="width: 90px; margin-bottom: 0;">
                <option :value="true">启用</option>
                <option :value="false">禁用</option>
              </select>
            </td>
            <td class="inline">
              <button @click="updateDepartment(row)">保存</button>
              <button class="danger" @click="deleteDepartment(row.id)">删除</button>
            </td>
          </tr>
          <tr v-if="departments.length === 0">
            <td colspan="4" class="muted">暂无部门数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../lib/http';

const classes = ref([]);
const departments = ref([]);
const error = ref('');
const success = ref('');

const newClass = reactive({
  className: '',
  majorName: ''
});

const newDepartment = reactive({
  departmentName: ''
});

function setMessage(type, text) {
  if (type === 'error') {
    error.value = text;
    success.value = '';
  } else {
    success.value = text;
    error.value = '';
  }
}

async function loadData() {
  try {
    const res = await request('/api/admin/org');
    classes.value = (res.data?.classes || []).map((r) => ({ ...r }));
    departments.value = (res.data?.departments || []).map((r) => ({ ...r }));
  } catch (e) {
    setMessage('error', e.message || '加载失败');
  }
}

async function createClass() {
  try {
    const res = await request('/api/admin/org/classes', {
      method: 'POST',
      data: { ...newClass }
    });
    setMessage('success', res.message || '创建成功');
    newClass.className = '';
    newClass.majorName = '';
    loadData();
  } catch (e) {
    setMessage('error', e.message || '创建失败');
  }
}

async function updateClass(row) {
  try {
    const res = await request(`/api/admin/org/classes/${row.id}`, {
      method: 'PUT',
      data: {
        className: row.className,
        majorName: row.majorName,
        enabled: row.enabled
      }
    });
    setMessage('success', res.message || '保存成功');
    loadData();
  } catch (e) {
    setMessage('error', e.message || '保存失败');
  }
}

async function deleteClass(id) {
  if (!window.confirm('确认删除该班级吗？')) {
    return;
  }
  try {
    const res = await request(`/api/admin/org/classes/${id}`, { method: 'DELETE' });
    setMessage('success', res.message || '删除成功');
    loadData();
  } catch (e) {
    setMessage('error', e.message || '删除失败');
  }
}

async function createDepartment() {
  try {
    const res = await request('/api/admin/org/departments', {
      method: 'POST',
      data: { ...newDepartment }
    });
    setMessage('success', res.message || '创建成功');
    newDepartment.departmentName = '';
    loadData();
  } catch (e) {
    setMessage('error', e.message || '创建失败');
  }
}

async function updateDepartment(row) {
  try {
    const res = await request(`/api/admin/org/departments/${row.id}`, {
      method: 'PUT',
      data: {
        departmentName: row.departmentName,
        enabled: row.enabled
      }
    });
    setMessage('success', res.message || '保存成功');
    loadData();
  } catch (e) {
    setMessage('error', e.message || '保存失败');
  }
}

async function deleteDepartment(id) {
  if (!window.confirm('确认删除该部门吗？')) {
    return;
  }
  try {
    const res = await request(`/api/admin/org/departments/${id}`, { method: 'DELETE' });
    setMessage('success', res.message || '删除成功');
    loadData();
  } catch (e) {
    setMessage('error', e.message || '删除失败');
  }
}

onMounted(loadData);
</script>
