<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="page-eyebrow">{{ title }}成绩管理</p>
        <h2 class="page-title">{{ isEdit ? `${title}单项成绩修改` : `${title}单项成绩录入` }}</h2>
        <p class="page-desc">
          {{ isEdit ? `当前正在修改已录入的${title}单项成绩。` : `这里用于补录${title}单项成绩，总分会由系统自动汇总。` }}
          修改入口也可以从“{{ title }}查询/修改”页直接进入。
        </p>
      </div>

      <div class="stat-grid edit-stats">
        <div class="stat-item">
          <span class="muted">当前模式</span>
          <strong>{{ isEdit ? '修改' : '录入' }}</strong>
          <small>{{ isEdit ? '保存后将自动更新对应总分' : '仅允许录入单项，不允许录入总分' }}</small>
        </div>
      </div>
    </section>

    <section class="card">
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <form @submit.prevent="save">
        <div class="toolbar-grid">
          <label>
            <span>学号</span>
            <input v-model.trim="form.studentNo" required />
          </label>
          <label>
            <span>姓名</span>
            <input v-model.trim="form.studentName" required />
          </label>
          <label>
            <span>班级</span>
            <input v-model.trim="form.className" required />
          </label>
          <label>
            <span>学期</span>
            <input v-model.trim="form.term" required />
          </label>
          <label>
            <span>成绩项目</span>
            <select v-model="form.itemCode" required>
              <option v-for="item in itemOptions" :key="item.itemCode" :value="item.itemCode">
                {{ item.itemName }}
              </option>
            </select>
          </label>
          <label>
            <span>分数</span>
            <input v-model.number="form.score" type="number" step="0.1" min="0" required />
          </label>
        </div>

        <div class="subtle-box tip-box">
          总分不提供直接编辑入口。录入或修改任意单项后，系统会自动同步该学生本学期的总分。
        </div>

        <div class="form-actions">
          <button type="submit">{{ isEdit ? '保存修改' : '提交录入' }}</button>
          <RouterLink class="ghost-link" :to="listPath">去查询/修改成绩</RouterLink>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { request } from '../../lib/http';

const route = useRoute();
const kind = computed(() => route.meta.kind || 'pe');
const title = computed(() => (kind.value === 'pe' ? '体育' : '智育'));
const id = computed(() => route.query.id || '');
const isEdit = computed(() => !!id.value);
const listPath = computed(() => (kind.value === 'pe' ? '/teacher/pe/list' : '/teacher/study/list'));

const form = reactive({
  studentNo: '',
  studentName: '',
  className: '',
  term: '2026-1',
  itemCode: '',
  score: 0
});

const error = ref('');
const success = ref('');
const itemOptions = ref([]);

function basePath() {
  return kind.value === 'pe' ? '/api/teacher/pe/scores' : '/api/teacher/study/scores';
}

function itemPath() {
  return kind.value === 'pe' ? '/api/teacher/pe/items' : '/api/teacher/study/items';
}

async function loadItems() {
  const res = await request(itemPath());
  const rows = Array.isArray(res.data) ? res.data : [];
  itemOptions.value = rows.filter((item) => !isTotalOption(item));
  if (itemOptions.value.length === 0) {
    form.itemCode = '';
    return;
  }
  const exists = itemOptions.value.some((item) => item.itemCode === form.itemCode);
  if (!exists) {
    form.itemCode = itemOptions.value[0].itemCode;
  }
}

async function loadDetail() {
  error.value = '';
  success.value = '';
  if (!isEdit.value) {
    form.studentNo = pickQueryValue(route.query.studentNo);
    form.studentName = pickQueryValue(route.query.studentName);
    form.className = pickQueryValue(route.query.className);
    form.term = pickQueryValue(route.query.term) || '2026-1';
    form.itemCode = itemOptions.value[0]?.itemCode || '';
    form.score = 0;
    return;
  }
  try {
    const res = await request(`${basePath()}/${id.value}`);
    const row = res.data;
    form.studentNo = row.studentNo;
    form.studentName = row.studentName;
    form.className = row.className;
    form.term = row.term;
    if (isTotalOption(row)) {
      form.itemCode = '';
      form.score = Number(row.totalScore ?? row.score ?? 0);
      error.value = `${title.value}总分由系统自动汇总，请返回查询页修改具体项目成绩`;
      return;
    }
    form.itemCode = row.itemCode || itemOptions.value[0]?.itemCode || '';
    form.score = Number(row.score || 0);
  } catch (e) {
    error.value = e.message || '加载失败';
  }
}

async function save() {
  error.value = '';
  success.value = '';
  if (!form.itemCode) {
    error.value = '暂无可录入的成绩项目，请联系管理员检查项目字典';
    return;
  }
  try {
    const payload = {
      studentNo: form.studentNo,
      studentName: form.studentName,
      className: form.className,
      term: form.term,
      itemCode: form.itemCode,
      score: Number(form.score)
    };

    let res;
    if (isEdit.value) {
      res = await request(`${basePath()}/${id.value}`, { method: 'PUT', data: payload });
    } else {
      res = await request(basePath(), { method: 'POST', data: payload });
    }

    success.value = res.message || '保存成功';
  } catch (e) {
    error.value = e.message || '保存失败';
  }
}

function isTotalOption(item) {
  const code = normalizeItemCode(item?.itemCode);
  const name = String(item?.itemName || '').trim();
  return code.endsWith('-TOTAL') || name.includes('总分');
}

function normalizeItemCode(value) {
  return String(value || '').trim().replaceAll('_', '-').toUpperCase();
}

function pickQueryValue(value) {
  return String(Array.isArray(value) ? value[0] || '' : value || '').trim();
}

onMounted(async () => {
  try {
    await loadItems();
    await loadDetail();
  } catch (e) {
    error.value = e.message || '加载失败';
  }
});

watch(
  () => route.fullPath,
  async () => {
    try {
      await loadItems();
      await loadDetail();
    } catch (e) {
      error.value = e.message || '加载失败';
    }
  }
);
</script>

<style scoped>
.edit-stats {
  min-width: 220px;
}

.tip-box {
  margin-top: 18px;
  color: var(--muted);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}
</style>
