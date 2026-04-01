<template>
  <div class="page-shell campus-import">
    <section class="page-header">
      <div>
        <p class="page-eyebrow">{{ title }}成绩管理</p>
        <h2 class="page-title">{{ title }}成绩导入</h2>
        <p class="page-desc">
          仅支持分项导入，系统会自动汇总总分。建议优先使用标准模板，避免字段顺序和编码错误。
        </p>
      </div>

      <div class="stat-grid import-stats">
        <div class="stat-item">
          <span class="muted">导入类型</span>
          <strong>{{ title }}分项</strong>
          <small>总分由系统汇总，不允许直接导入总分</small>
        </div>
        <div class="stat-item">
          <span class="muted">模板列顺序</span>
          <strong>6 列</strong>
          <small>studentNo, studentName, className, term, itemCode, score</small>
        </div>
      </div>
    </section>

    <section class="card">
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>

      <form class="import-form" @submit.prevent="submitImport">
        <label>
          <span>上传 CSV</span>
          <input type="file" accept=".csv" @change="onFileChange" required />
        </label>

        <div v-if="fileName" class="subtle-box file-box">
          当前文件：{{ fileName }}
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="!file">开始导入</button>
          <button
            type="button"
            class="ghost-link"
            :disabled="downloadingTemplate"
            @click="downloadTemplate"
          >
            {{ downloadingTemplate ? '下载中...' : '下载模板' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { request } from '../../lib/http';
import { getAccessToken } from '../../lib/token';

const route = useRoute();
const apiBase = import.meta.env.VITE_API_BASE || '';
const kind = computed(() => route.meta.kind || 'pe');
const title = computed(() => (kind.value === 'pe' ? '体育' : '智育'));
const templatePath = computed(() => (kind.value === 'pe'
  ? '/api/templates/pe-scores-import.csv'
  : '/api/templates/study-scores-import.csv'));

const file = ref(null);
const fileName = computed(() => file.value?.name || '');
const downloadingTemplate = ref(false);
const error = ref('');
const success = ref('');

function onFileChange(event) {
  file.value = event.target.files?.[0] || null;
}

function pickDownloadFilename(contentDisposition, fallbackName) {
  if (!contentDisposition) {
    return fallbackName;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch (e) {
      return utf8Match[1];
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1] || fallbackName;
}

async function downloadTemplate() {
  error.value = '';
  success.value = '';
  downloadingTemplate.value = true;

  try {
    const headers = {};
    const accessToken = getAccessToken();
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${apiBase}${templatePath.value}`, {
      method: 'GET',
      credentials: 'include',
      headers
    });

    if (!response.ok) {
      const contentType = response.headers.get('content-type') || '';
      let message = `模板下载失败 (${response.status})`;
      if (contentType.includes('application/json')) {
        const payload = await response.json().catch(() => null);
        if (payload?.message) {
          message = payload.message;
        }
      } else {
        const text = await response.text();
        if (text) {
          message = text;
        }
      }
      throw new Error(message);
    }

    const blob = await response.blob();
    if (!blob || blob.size === 0) {
      throw new Error('模板文件为空，请稍后重试');
    }

    const fallbackName = kind.value === 'pe' ? 'pe_scores_import_template.csv' : 'study_scores_import_template.csv';
    const fileNameToSave = pickDownloadFilename(response.headers.get('content-disposition') || '', fallbackName);
    const downloadUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.style.display = 'none';
    anchor.href = downloadUrl;
    anchor.download = fileNameToSave;
    document.body.appendChild(anchor);
    anchor.click();
    window.setTimeout(() => {
      URL.revokeObjectURL(downloadUrl);
      anchor.remove();
    }, 1500);
    success.value = '模板下载成功';
  } catch (e) {
    error.value = e.message || '模板下载失败';
  } finally {
    downloadingTemplate.value = false;
  }
}

async function submitImport() {
  error.value = '';
  success.value = '';
  if (!file.value) {
    error.value = '请选择 CSV 文件';
    return;
  }

  try {
    const form = new FormData();
    form.append('file', file.value);
    const base = kind.value === 'pe' ? '/api/teacher/pe/scores/import' : '/api/teacher/study/scores/import';
    const res = await request(base, { method: 'POST', data: form, isForm: true });
    const summary = res.data || {};
    success.value = `导入完成：批次 ${summary.batchNo || '-'}，成功 ${summary.success ?? 0}，失败 ${summary.failed ?? 0}`;
    if ((summary.errors || []).length > 0) {
      error.value = `有 ${(summary.errors || []).length} 条失败记录，请到管理员“导入批次”查看详情`;
    }
  } catch (e) {
    error.value = e.message || '导入失败';
  }
}
</script>

<style scoped>
.import-stats {
  min-width: 420px;
}

.campus-import .page-header {
  border-left-color: #6f95d8;
  background: linear-gradient(180deg, #ffffff 0%, #f2f7ff 100%);
  box-shadow: 0 10px 22px rgba(34, 58, 104, 0.1);
}

.campus-import .page-eyebrow {
  color: #466aa9;
}

.campus-import .page-title {
  color: #233a63;
}

.campus-import .page-desc {
  color: #637496;
}

.campus-import .stat-item {
  background: linear-gradient(180deg, #ffffff 0%, #eef4ff 100%);
  border-color: #d5e0f1;
}

.campus-import .stat-item strong {
  font-size: 24px;
  color: #2f4f82;
}

.campus-import .card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 255, 0.98) 100%);
  border-color: #d7e2f3;
}

.import-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.file-box {
  margin-top: 2px;
  color: var(--muted);
  border-color: #d0ddf1;
  background: linear-gradient(180deg, #f9fbff 0%, #edf3ff 100%);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.form-actions button {
  background: linear-gradient(135deg, #2f6fda 0%, #4f86e3 100%);
  box-shadow: 0 8px 16px rgba(47, 111, 218, 0.22);
}

.form-actions .ghost-link {
  border-color: #cbdaf1;
  background: #f8fbff;
  color: #4a6596;
}

@media (max-width: 960px) {
  .import-stats {
    min-width: 0;
  }
}
</style>
