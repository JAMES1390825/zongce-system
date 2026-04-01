<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="page-eyebrow">管理员工作台</p>
        <h2 class="page-title">老师管理</h2>
        <p class="page-desc">
          统一维护体育老师、学习部老师和辅导员账号。辅导员账号必须绑定负责班级，系统会自动按班级执行数据权限控制。
        </p>
      </div>

      <div class="stat-grid teachers-stats">
        <div class="stat-item">
          <span class="muted">当前记录</span>
          <strong>{{ rows.length }}</strong>
          <small>筛选结果中的老师账号总数</small>
        </div>
        <div class="stat-item">
          <span class="muted">可管理角色</span>
          <strong>{{ roles.length }}</strong>
          <small>体育老师 / 学习部老师 / 辅导员</small>
        </div>
      </div>
    </section>

    <section class="card">
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <form class="toolbar-grid teachers-filter" @submit.prevent="loadRows">
        <label>
          <span>角色筛选</span>
          <select v-model="filterRole" style="margin-bottom: 0;">
            <option value="">全部角色</option>
            <option v-for="r in roles" :key="r" :value="r">{{ r }}</option>
          </select>
        </label>
        <label>
          <span>部门关键词</span>
          <input v-model.trim="department" placeholder="如 体育部" style="margin-bottom: 0;" />
        </label>
        <div class="teachers-filter-actions">
          <button type="submit">筛选</button>
          <button type="button" class="secondary" @click="clearFilter">清空</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h3 class="section-title">新增老师</h3>
      <form @submit.prevent="createTeacher">
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
            <label>角色</label>
            <select v-model="createForm.role" required>
              <option v-for="r in roles" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>
          <div>
            <label>部门</label>
            <input v-model.trim="createForm.department" />
          </div>
          <div>
            <label>负责班级（辅导员必填）</label>
            <input v-model.trim="createForm.className" :required="isCounselorRole(createForm.role)" />
          </div>
        </div>
        <div class="teachers-create-actions">
          <button type="submit">创建老师</button>
        </div>
      </form>
    </section>

    <section class="card">
      <h3 class="section-title">老师列表</h3>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>账号</th>
              <th>姓名</th>
              <th>角色</th>
              <th>部门</th>
              <th>负责班级</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td><input v-model.trim="row.username" style="width: 120px; margin-bottom: 0;" /></td>
              <td><input v-model.trim="row.name" style="width: 120px; margin-bottom: 0;" /></td>
              <td>
                <select v-model="row.role" style="width: 150px; margin-bottom: 0;">
                  <option v-for="r in roles" :key="r" :value="r">{{ r }}</option>
                </select>
              </td>
              <td><input v-model.trim="row.department" style="width: 120px; margin-bottom: 0;" /></td>
              <td><input v-model.trim="row.className" :required="isCounselorRole(row.role)" style="width: 120px; margin-bottom: 0;" /></td>
              <td>
                <select v-model="row.enabled" style="width: 90px; margin-bottom: 0;">
                  <option :value="true">启用</option>
                  <option :value="false">禁用</option>
                </select>
              </td>
              <td class="inline teachers-table-actions">
                <button @click="updateRow(row)">保存</button>
                <button class="secondary" @click="resetPassword(row.id)">重置密码</button>
                <button class="danger" @click="deleteRow(row.id)">删除</button>
              </td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="8" class="muted">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../lib/http';

const roles = ['TEACHER_PE', 'TEACHER_STUDY', 'COUNSELOR'];
const rows = ref([]);
const filterRole = ref('');
const department = ref('');
const error = ref('');
const success = ref('');

const createForm = reactive({
  username: '',
  name: '',
  role: 'TEACHER_PE',
  department: '',
  className: ''
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

async function loadRows() {
  try {
    const params = new URLSearchParams();
    if (filterRole.value) params.append('role', filterRole.value);
    if (department.value) params.append('department', department.value);
    const query = params.toString() ? `?${params.toString()}` : '';

    const res = await request(`/api/admin/teachers${query}`);
    rows.value = (res.data || []).map((r) => ({ ...r }));
  } catch (e) {
    setMessage('error', e.message || '加载失败');
  }
}

function clearFilter() {
  filterRole.value = '';
  department.value = '';
  loadRows();
}

function isCounselorRole(role) {
  return role === 'COUNSELOR';
}

function assertTeacherPayload(payload) {
  if (isCounselorRole(payload.role) && !String(payload.className || '').trim()) {
    throw new Error('辅导员必须填写负责班级');
  }
}

async function createTeacher() {
  try {
    assertTeacherPayload(createForm);
    const res = await request('/api/admin/teachers', {
      method: 'POST',
      data: createForm
    });
    setMessage('success', res.message || '创建成功');
    createForm.username = '';
    createForm.name = '';
    createForm.role = 'TEACHER_PE';
    createForm.department = '';
    createForm.className = '';
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '创建失败');
  }
}

async function updateRow(row) {
  try {
    assertTeacherPayload(row);
    const res = await request(`/api/admin/teachers/${row.id}`, {
      method: 'PUT',
      data: {
        username: row.username,
        name: row.name,
        role: row.role,
        department: row.department,
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
    const res = await request(`/api/admin/teachers/${id}/reset-password`, { method: 'POST' });
    setMessage('success', res.message || '重置成功');
  } catch (e) {
    setMessage('error', e.message || '重置失败');
  }
}

async function deleteRow(id) {
  if (!window.confirm('确认删除该老师账号吗？')) {
    return;
  }
  try {
    const res = await request(`/api/admin/teachers/${id}`, { method: 'DELETE' });
    setMessage('success', res.message || '删除成功');
    loadRows();
  } catch (e) {
    setMessage('error', e.message || '删除失败');
  }
}

onMounted(loadRows);
</script>

<style scoped>
.teachers-stats {
  min-width: 420px;
}

.teachers-filter {
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1fr) auto;
}

.teachers-filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: end;
}

.teachers-create-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.teachers-table-actions {
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .teachers-stats {
    min-width: 0;
  }

  .teachers-filter {
    grid-template-columns: 1fr;
  }

  .teachers-table-actions {
    justify-content: flex-start;
  }
}
</style>
