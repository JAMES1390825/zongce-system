<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="page-eyebrow">{{ title }}成绩管理</p>
        <h2 class="page-title">{{ title }}成绩查询与修改</h2>
        <p class="page-desc">
          先按班级、学期查询，再在对应学生的单项记录中进行修改。总分由系统自动汇总，只能查看，不能直接编辑。
        </p>
      </div>

      <div class="stat-grid list-stats">
        <div class="stat-item">
          <span class="muted">学生记录</span>
          <strong>{{ summary.studentCount }}</strong>
          <small>当前筛选下的学生学期组合</small>
        </div>
        <div class="stat-item">
          <span class="muted">可改单项</span>
          <strong>{{ summary.editableItemCount }}</strong>
          <small>存在单项记录，可直接修改</small>
        </div>
        <div class="stat-item">
          <span class="muted">仅有总分</span>
          <strong>{{ summary.legacyOnlyCount }}</strong>
          <small>需要先补录单项后再汇总</small>
        </div>
      </div>
    </section>

    <section class="card">
      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <form class="toolbar-grid list-toolbar" @submit.prevent="loadRows">
        <label>
          <span>班级</span>
          <input v-model.trim="className" placeholder="例如 软件221" />
        </label>
        <label>
          <span>学期</span>
          <input v-model.trim="term" placeholder="例如 2026-1" />
        </label>
        <div class="toolbar-actions">
          <button type="submit">查询成绩</button>
          <RouterLink class="ghost-link" :to="editPath">补录单项</RouterLink>
        </div>
      </form>

      <div class="subtle-box helper-box">
        修改入口就在下方学生卡片内。如果当前只有历史总分，系统会显示“补录单项”按钮，而不会显示“修改单项”。
      </div>
    </section>

    <section v-if="groupedRows.length === 0" class="card empty-state">
      <h3 class="section-title">暂无成绩记录</h3>
      <p class="section-desc">可以调整筛选条件，或者直接进入录入页补录单项成绩。</p>
      <RouterLink class="primary-link" :to="editPath">去补录单项</RouterLink>
    </section>

    <section v-else class="record-list">
      <article v-for="group in groupedRows" :key="group.key" class="card record-card">
        <div class="record-head">
          <div>
            <div class="record-title-row">
              <h3 class="record-title">{{ group.studentName || '未命名学生' }}</h3>
              <span class="record-id">{{ group.studentNo || '-' }}</span>
            </div>
            <div class="record-meta">
              <span>{{ group.className || '未填写班级' }}</span>
              <span>{{ group.term || '未填写学期' }}</span>
              <span>最近录入：{{ group.operator || '-' }}</span>
            </div>
          </div>

          <div class="record-summary">
            <div class="total-box">
              <span>总分</span>
              <strong>{{ formatScore(group.totalScore) }}</strong>
            </div>
            <span v-if="group.items.length > 0" class="state-pill state-pill-ok">可修改 {{ group.items.length }} 项</span>
            <span v-else class="state-pill state-pill-warn">仅有历史总分</span>
          </div>
        </div>

        <div v-if="group.items.length > 0" class="item-table-wrap">
          <table class="item-table">
            <thead>
              <tr>
                <th>项目</th>
                <th>单项分</th>
                <th>录入人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in group.items" :key="item.id">
                <td>{{ item.itemName || `${title}单项` }}</td>
                <td>{{ formatScore(item.score) }}</td>
                <td>{{ item.createdBy || '-' }}</td>
                <td>
                  <RouterLink class="table-action" :to="{ path: editPath, query: { id: item.id } }">修改单项</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="legacy-panel">
          <div>
            <h4>当前只有历史总分记录</h4>
            <p>请先补录该学生当前学期的具体{{ title === '体育' ? '体育分项' : '智育科目' }}，系统会自动重新汇总总分。</p>
          </div>
          <RouterLink class="primary-link" :to="prefillCreateLink(group)">补录单项</RouterLink>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { request } from '../../lib/http';

const route = useRoute();
const kind = computed(() => route.meta.kind || 'pe');
const title = computed(() => (kind.value === 'pe' ? '体育' : '智育'));
const editPath = computed(() => (kind.value === 'pe' ? '/teacher/pe/edit' : '/teacher/study/edit'));

const className = ref('');
const term = ref('');
const rows = ref([]);
const error = ref('');

const groupedRows = computed(() => buildGroups(rows.value));

const summary = computed(() => ({
  studentCount: groupedRows.value.length,
  editableItemCount: groupedRows.value.reduce((sum, group) => sum + group.items.length, 0),
  legacyOnlyCount: groupedRows.value.filter((group) => group.items.length === 0).length
}));

function basePath() {
  return kind.value === 'pe' ? '/api/teacher/pe/scores' : '/api/teacher/study/scores';
}

async function loadRows() {
  error.value = '';
  try {
    const params = new URLSearchParams();
    if (className.value) params.append('className', className.value);
    if (term.value) params.append('term', term.value);
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`${basePath()}${query}`);
    rows.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    error.value = e.message || '加载失败';
  }
}

function buildGroups(sourceRows) {
  const map = new Map();

  for (const row of sourceRows || []) {
    const key = `${row.studentNo || ''}::${row.className || ''}::${row.term || ''}`;
    let group = map.get(key);

    if (!group) {
      group = {
        key,
        studentNo: row.studentNo || '',
        studentName: row.studentName || '',
        className: row.className || '',
        term: row.term || '',
        totalScore: row.totalScore ?? row.score ?? 0,
        operator: row.createdBy || '',
        items: []
      };
      map.set(key, group);
    }

    if (row.totalScore !== undefined && row.totalScore !== null) {
      group.totalScore = row.totalScore;
    }
    if (row.createdBy) {
      group.operator = row.createdBy;
    }

    if (!isTotalRow(row)) {
      group.items.push(row);
    } else if (group.items.length === 0 && (group.totalScore === undefined || group.totalScore === null)) {
      group.totalScore = row.score ?? 0;
    }
  }

  return Array.from(map.values())
    .map((group) => ({
      ...group,
      items: [...group.items].sort((a, b) => String(a.itemName || '').localeCompare(String(b.itemName || ''), 'zh-CN'))
    }))
    .sort((a, b) => {
      const termCompare = String(b.term || '').localeCompare(String(a.term || ''), 'zh-CN');
      if (termCompare !== 0) return termCompare;
      const classCompare = String(a.className || '').localeCompare(String(b.className || ''), 'zh-CN');
      if (classCompare !== 0) return classCompare;
      return String(a.studentNo || '').localeCompare(String(b.studentNo || ''), 'zh-CN');
    });
}

function isTotalRow(row) {
  const code = normalizeItemCode(row?.itemCode);
  const name = String(row?.itemName || '').trim();
  return code.endsWith('-TOTAL') || name.includes('总分');
}

function normalizeItemCode(value) {
  return String(value || '').trim().replaceAll('_', '-').toUpperCase();
}

function formatScore(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const num = Number(value);
  if (Number.isNaN(num)) {
    return String(value);
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(1);
}

function prefillCreateLink(group) {
  return {
    path: editPath.value,
    query: {
      studentNo: group.studentNo,
      studentName: group.studentName,
      className: group.className,
      term: group.term
    }
  };
}

onMounted(loadRows);

watch(
  () => route.fullPath,
  () => {
    loadRows();
  }
);
</script>

<style scoped>
.list-stats {
  min-width: 420px;
}

.list-toolbar {
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.8fr) auto;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: end;
}

.helper-box {
  margin-top: 16px;
  color: #52616d;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-card {
  margin-bottom: 0;
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.record-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.record-title {
  margin: 0;
  font-size: 24px;
}

.record-id {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 13px;
  font-weight: 700;
}

.record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 10px;
  color: var(--muted);
}

.record-summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.total-box {
  min-width: 118px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--card-soft);
  border: 1px solid var(--line);
}

.total-box span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.total-box strong {
  display: block;
  margin-top: 4px;
  font-size: 28px;
  color: var(--text);
}

.state-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.state-pill-ok {
  background: var(--primary-soft);
  color: var(--primary-dark);
}

.state-pill-warn {
  background: #edf2ff;
  color: #5067a1;
}

.item-table-wrap {
  overflow-x: auto;
}

.item-table th:last-child,
.item-table td:last-child {
  text-align: right;
}

.table-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-weight: 700;
}

.legacy-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 14px;
  background: #faf6ee;
  border: 1px dashed #cbb69a;
}

.legacy-panel h4 {
  margin: 0;
  font-size: 18px;
  color: #6f5133;
}

.legacy-panel p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .list-stats {
    min-width: 0;
  }

  .list-toolbar {
    grid-template-columns: 1fr;
  }

  .record-head,
  .legacy-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .record-summary {
    align-items: flex-start;
  }

  .item-table th:last-child,
  .item-table td:last-child {
    text-align: left;
  }
}
</style>
