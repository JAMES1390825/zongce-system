<template>
  <div>
    <div class="card">
      <h2>我的成绩</h2>
      <div class="inline" style="margin-bottom: 12px;">
        <button type="button" :class="mode === 'summary' ? '' : 'secondary'" @click="mode = 'summary'">综测总览</button>
        <button type="button" :class="mode === 'transcript' ? '' : 'secondary'" @click="mode = 'transcript'">单科成绩单</button>
      </div>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
    </div>

    <div v-if="mode === 'summary'">
      <div class="card">
        <h3 style="margin-top: 0;">综测对比</h3>
        <div v-if="rows.length >= 2">
          <form class="inline" style="margin-bottom: 12px;" @submit.prevent>
            <div>
              <label>当前学期</label>
              <select v-model="currentTerm" style="width: 160px; margin-bottom: 0;">
                <option v-for="term in terms" :key="`current-${term}`" :value="term">{{ term }}</option>
              </select>
            </div>
            <div>
              <label>对比学期</label>
              <select v-model="compareTerm" style="width: 160px; margin-bottom: 0;">
                <option v-for="term in compareTermOptions" :key="`compare-${term}`" :value="term">{{ term }}</option>
              </select>
            </div>
          </form>

          <table>
            <thead>
              <tr>
                <th>指标</th>
                <th>{{ currentTerm }}</th>
                <th>{{ compareTerm }}</th>
                <th>变化值（当前-对比）</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="metric in compareMetrics" :key="metric.key">
                <td>{{ metric.label }}</td>
                <td>{{ metric.current }}</td>
                <td>{{ metric.compare }}</td>
                <td>
                  <span :class="deltaClass(metric.delta, metric.betterWhenLower)">
                    {{ metric.deltaText }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-else-if="rows.length === 1" class="muted">仅有 1 条成绩记录，至少需要 2 个学期才能进行对比。</p>
      </div>

      <div class="card">
        <h3 style="margin-top: 0;">历史成绩明细</h3>
        <table>
          <thead>
            <tr>
              <th>学期</th>
              <th>智育</th>
              <th>体育</th>
              <th>德育</th>
              <th>技能</th>
              <th>总分</th>
              <th>班级排名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.id">
              <td>{{ r.term }}</td>
              <td>{{ r.studyScore }}</td>
              <td>{{ r.peScore }}</td>
              <td>{{ r.moralScore }}</td>
              <td>{{ r.skillScore }}</td>
              <td>{{ r.totalScore }}</td>
              <td>{{ r.rankNo }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="7" class="muted">暂无成绩，请等待辅导员重算后查看</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else>
      <div class="card">
        <h3 style="margin-top: 0;">单科成绩单</h3>
        <div class="inline" style="margin-bottom: 12px;">
          <div>
            <label>学期</label>
            <select v-model="transcriptTerm" style="width: 180px; margin-bottom: 0;">
              <option v-for="term in transcriptTermOptions" :key="`transcript-${term}`" :value="term">{{ term }}</option>
            </select>
          </div>
          <button type="button" class="secondary" @click="exportTranscript" :disabled="!transcriptTerm">导出成绩单</button>
        </div>
        <div v-if="transcriptLoading" class="muted">正在加载单科成绩...</div>
        <div v-if="transcriptError" class="alert alert-error">{{ transcriptError }}</div>

        <div v-if="transcript.summary" class="inline" style="gap: 20px; margin-bottom: 12px;">
          <span>状态：<b>{{ transcript.resultStatus === 'COMPLETE' ? '完整' : '待补全' }}</b></span>
          <span>完成度：<b>{{ transcript.completionRate }}%</b></span>
          <span>已录必填项：<b>{{ transcript.requiredRecordedCount }}/{{ transcript.requiredCount }}</b></span>
        </div>

        <div v-if="transcript.missingRequiredItems?.length" class="alert alert-error">
          缺少必填项目：
          {{ transcript.missingRequiredItems.map((r) => r.itemName).join('、') }}
        </div>

        <h4 style="margin-top: 16px;">智育项目</h4>
        <table>
          <thead>
            <tr>
              <th>项目名称</th>
              <th>原始分</th>
              <th>折算分</th>
              <th>状态</th>
              <th>来源</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in transcript.studyItems" :key="`study-${row.itemCode}`">
              <td>{{ row.itemName }}</td>
              <td>{{ showValue(row.rawScore) }}</td>
              <td>{{ showValue(row.standardScore) }}</td>
              <td>{{ formatEntryStatus(row.status) }}</td>
              <td>{{ formatEntrySource(row.source) }}</td>
            </tr>
            <tr v-if="!transcript.studyItems?.length">
              <td colspan="5" class="muted">暂无智育单科数据</td>
            </tr>
          </tbody>
        </table>

        <h4 style="margin-top: 16px;">体育项目</h4>
        <table>
          <thead>
            <tr>
              <th>项目名称</th>
              <th>原始分</th>
              <th>折算分</th>
              <th>状态</th>
              <th>来源</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in transcript.peItems" :key="`pe-${row.itemCode}`">
              <td>{{ row.itemName }}</td>
              <td>{{ showValue(row.rawScore) }}</td>
              <td>{{ showValue(row.standardScore) }}</td>
              <td>{{ formatEntryStatus(row.status) }}</td>
              <td>{{ formatEntrySource(row.source) }}</td>
            </tr>
            <tr v-if="!transcript.peItems?.length">
              <td colspan="5" class="muted">暂无体育单科数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3 style="margin-top: 0;">体育计算过程</h3>
        <div v-if="peBreakdownLoading" class="muted">正在加载体育分项过程...</div>
        <div v-if="peBreakdownError" class="alert alert-error">{{ peBreakdownError }}</div>

        <table>
          <thead>
            <tr>
              <th>分项名称</th>
              <th>原始值</th>
              <th>换算分</th>
              <th>权重</th>
              <th>加权分</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in peBreakdownRows" :key="`bd-${row.componentCode}`">
              <td>{{ row.componentName }}</td>
              <td>{{ showValue(row.rawValue) }}</td>
              <td>{{ showValue(row.convertedScore) }}</td>
              <td>{{ showValue(row.weight) }}</td>
              <td>{{ showValue(row.weightedScore) }}</td>
              <td>{{ showValue(row.formulaSnapshot) }}</td>
            </tr>
            <tr v-if="!peBreakdownRows.length">
              <td colspan="6" class="muted">当前学期暂无体育过程数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { request } from '../../lib/http';

const mode = ref('summary');
const rows = ref([]);
const error = ref('');
const currentTerm = ref('');
const compareTerm = ref('');

const transcriptTerm = ref('');
const transcriptLoading = ref(false);
const transcriptError = ref('');
const transcript = reactive({
  summary: null,
  studyItems: [],
  peItems: [],
  missingRequiredItems: [],
  requiredCount: 0,
  requiredRecordedCount: 0,
  completionRate: '0.00',
  resultStatus: 'INCOMPLETE'
});

const peBreakdownLoading = ref(false);
const peBreakdownError = ref('');
const peBreakdownRows = ref([]);

const terms = computed(() => rows.value.map((r) => r.term));
const transcriptTermOptions = computed(() => {
  const set = new Set();
  rows.value.forEach((r) => set.add(r.term));
  if (transcriptTerm.value) {
    set.add(transcriptTerm.value);
  }
  return Array.from(set);
});

const compareTermOptions = computed(() => terms.value.filter((t) => t !== currentTerm.value));

const currentRow = computed(() => rows.value.find((r) => r.term === currentTerm.value) || null);
const compareRow = computed(() => rows.value.find((r) => r.term === compareTerm.value) || null);

const compareMetrics = computed(() => {
  if (!currentRow.value || !compareRow.value) {
    return [];
  }

  const metrics = [
    { key: 'totalScore', label: '总分', betterWhenLower: false },
    { key: 'studyScore', label: '智育', betterWhenLower: false },
    { key: 'peScore', label: '体育', betterWhenLower: false },
    { key: 'moralScore', label: '德育', betterWhenLower: false },
    { key: 'skillScore', label: '技能', betterWhenLower: false },
    { key: 'rankNo', label: '班级排名', betterWhenLower: true }
  ];

  return metrics.map((metric) => {
    const current = Number(currentRow.value[metric.key] ?? 0);
    const compare = Number(compareRow.value[metric.key] ?? 0);
    const delta = current - compare;
    const formattedDelta = formatDelta(delta, metric.betterWhenLower);

    return {
      ...metric,
      current: showValue(currentRow.value[metric.key]),
      compare: showValue(compareRow.value[metric.key]),
      delta,
      deltaText: formattedDelta
    };
  });
});

function showValue(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return value;
}

function formatEntryStatus(status) {
  if (!status) return '-';
  if (status === 'MISSING') return '未录入';
  if (status === 'RECORDED') return '已录入';
  if (status === 'LOCKED') return '已锁定';
  return status;
}

function formatEntrySource(source) {
  if (!source) return '-';
  if (source === 'IMPORT') return '批量导入';
  if (source === 'MANUAL') return '人工录入';
  if (source === 'MIGRATION') return '历史数据';
  return source;
}

function formatDelta(delta, betterWhenLower) {
  if (delta === 0) {
    return '持平';
  }

  const sign = delta > 0 ? '+' : '';

  if (betterWhenLower) {
    if (delta < 0) {
      return `${sign}${delta}（提升）`;
    }
    return `${sign}${delta}（下降）`;
  }

  if (delta > 0) {
    return `${sign}${delta}（提升）`;
  }
  return `${sign}${delta}（下降）`;
}

function deltaClass(delta, betterWhenLower) {
  if (delta === 0) {
    return 'muted';
  }

  const improved = betterWhenLower ? delta < 0 : delta > 0;
  return improved ? 'delta-up' : 'delta-down';
}

function initCompareTerms() {
  if (rows.value.length === 0) {
    currentTerm.value = '';
    compareTerm.value = '';
    return;
  }

  currentTerm.value = rows.value[0].term;
  compareTerm.value = rows.value[1]?.term || '';
}

async function loadRows() {
  error.value = '';
  try {
    const res = await request('/api/student/scores');
    rows.value = res.data || [];
    initCompareTerms();
    if (!transcriptTerm.value) {
      transcriptTerm.value = rows.value[0]?.term || '';
    }
    if (transcriptTerm.value) {
      await Promise.all([loadTranscript(), loadPeBreakdown()]);
    }
  } catch (e) {
    error.value = e.message || '加载失败';
  }
}

async function loadTranscript() {
  if (!transcriptTerm.value) {
    return;
  }
  transcriptError.value = '';
  transcriptLoading.value = true;
  try {
    const res = await request(`/api/student/transcript?term=${encodeURIComponent(transcriptTerm.value)}`);
    const data = res.data || {};
    transcript.summary = data.summary || null;
    transcript.studyItems = data.studyItems || [];
    transcript.peItems = data.peItems || [];
    transcript.missingRequiredItems = data.missingRequiredItems || [];
    transcript.requiredCount = Number(data.requiredCount || 0);
    transcript.requiredRecordedCount = Number(data.requiredRecordedCount || 0);
    transcript.completionRate = data.completionRate ?? '0.00';
    transcript.resultStatus = data.resultStatus || 'INCOMPLETE';
  } catch (e) {
    transcriptError.value = e.message || '加载单科成绩失败';
  } finally {
    transcriptLoading.value = false;
  }
}

async function loadPeBreakdown() {
  if (!transcriptTerm.value) {
    return;
  }
  peBreakdownError.value = '';
  peBreakdownLoading.value = true;
  try {
    const res = await request(`/api/student/transcript/pe-breakdown?term=${encodeURIComponent(transcriptTerm.value)}`);
    peBreakdownRows.value = res.data?.rows || [];
  } catch (e) {
    peBreakdownError.value = e.message || '加载体育过程失败';
  } finally {
    peBreakdownLoading.value = false;
  }
}

function exportTranscript() {
  if (!transcriptTerm.value) {
    return;
  }
  window.open(`/api/student/transcript/export?term=${encodeURIComponent(transcriptTerm.value)}`, '_blank');
}

watch(compareTermOptions, (options) => {
  if (!compareTerm.value || compareTerm.value === currentTerm.value) {
    compareTerm.value = options[0] || '';
  }
});

watch(transcriptTerm, async (value, oldValue) => {
  if (!value || value === oldValue) {
    return;
  }
  await Promise.all([loadTranscript(), loadPeBreakdown()]);
});

onMounted(loadRows);
</script>
