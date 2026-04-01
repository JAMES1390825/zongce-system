<template>
  <div class="card">
    <h2>{{ title }}</h2>
    <p class="muted">提交后进入待审核状态</p>

    <div class="inline" style="margin-bottom: 10px;">
      <RouterLink to="/student/declare/moral">德育申报</RouterLink>
      <RouterLink to="/student/declare/skill">技能申报</RouterLink>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="success" class="alert alert-success">{{ success }}</div>

    <form @submit.prevent="submitForm">
      <div class="grid">
        <div>
          <label>类型</label>
          <input :value="typeLabel" disabled />
        </div>
        <div>
          <label>学期</label>
          <input v-model.trim="term" required :class="{ 'input-error': fieldErrors.term }" placeholder="例如：2026-1" />
          <p v-if="fieldErrors.term" class="field-error">{{ fieldErrors.term }}</p>
        </div>
        <div>
          <label>标准项目</label>
          <select v-model="itemCode" required :class="{ 'input-error': fieldErrors.itemCode }">
            <option disabled value="">请选择项目</option>
            <option v-for="item in itemOptions" :key="item.itemCode" :value="item.itemCode">
              {{ item.itemName }}
            </option>
            <option value="OTHER">其他（手动填写）</option>
          </select>
          <p v-if="fieldErrors.itemCode" class="field-error">{{ fieldErrors.itemCode }}</p>
        </div>
        <div>
          <label>项目名称</label>
          <input
            v-model.trim="itemName"
            required
            :readonly="itemCode && itemCode !== 'OTHER'"
            :class="{ 'input-error': fieldErrors.itemName }"
            :placeholder="itemCode === 'OTHER' ? '请填写自定义项目名称' : '选择标准项目后自动带出'"
          />
          <p v-if="fieldErrors.itemName" class="field-error">{{ fieldErrors.itemName }}</p>
        </div>
        <div>
          <label>分值</label>
          <input v-model.number="points" type="number" step="0.1" min="0.1" max="100" required :class="{ 'input-error': fieldErrors.points }" />
          <p v-if="fieldErrors.points" class="field-error">{{ fieldErrors.points }}</p>
        </div>
      </div>

      <label>说明</label>
      <textarea v-model="description"></textarea>

      <label>附件（可选）</label>
      <input ref="fileInputRef" type="file" accept=".pdf,.jpg,.jpeg,.png" @change="onFileChange" />
      <p class="muted">支持 PDF/JPG/PNG，最大 5MB</p>
      <p v-if="fieldErrors.file" class="field-error">{{ fieldErrors.file }}</p>

      <div v-if="file" class="file-chip inline" style="margin-bottom: 10px;">
        <span>已选择：{{ file.name }}（{{ formatFileSize(file.size) }}）</span>
        <button type="button" class="secondary" @click="clearFile">移除附件</button>
      </div>

      <button type="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交申报' }}</button>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { request } from '../../lib/http';

const route = useRoute();
const type = computed(() => route.meta.type || 'MORAL');
const title = computed(() => (type.value === 'MORAL' ? '德育分申报' : '技能分申报'));
const typeLabel = computed(() => (type.value === 'MORAL' ? '德育' : '技能'));

const term = ref('2026-1');
const itemCode = ref('');
const itemName = ref('');
const points = ref(1);
const description = ref('');
const file = ref(null);
const fileInputRef = ref(null);
const submitting = ref(false);
const itemOptions = ref([]);

const error = ref('');
const success = ref('');

const fieldErrors = ref({
  term: '',
  itemCode: '',
  itemName: '',
  points: '',
  file: ''
});

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png'];

function clearFieldErrors() {
  fieldErrors.value = {
    term: '',
    itemCode: '',
    itemName: '',
    points: '',
    file: ''
  };
}

function validateForm() {
  clearFieldErrors();

  const termPattern = /^\d{4}-[1-2]$/;
  if (!term.value || !termPattern.test(term.value)) {
    fieldErrors.value.term = '学期格式应为 yyyy-1 或 yyyy-2';
  }

  if (!itemCode.value) {
    fieldErrors.value.itemCode = '请选择标准项目';
  }

  if (!itemName.value || itemName.value.length < 2) {
    fieldErrors.value.itemName = '项目名称至少 2 个字符';
  }

  const p = Number(points.value);
  if (!Number.isFinite(p) || p < 0.1 || p > 100) {
    fieldErrors.value.points = '分值范围需在 0.1 到 100 之间';
  }

  if (file.value) {
    const name = file.value.name || '';
    const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : '';
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      fieldErrors.value.file = '附件格式仅支持 PDF/JPG/PNG';
    }
    if (file.value.size > MAX_FILE_SIZE) {
      fieldErrors.value.file = '附件大小不能超过 5MB';
    }
  }

  return !fieldErrors.value.term
    && !fieldErrors.value.itemCode
    && !fieldErrors.value.itemName
    && !fieldErrors.value.points
    && !fieldErrors.value.file;
}

function formatFileSize(size) {
  if (!Number.isFinite(size) || size <= 0) return '0KB';
  if (size < 1024 * 1024) return `${Math.round(size / 1024)}KB`;
  return `${(size / (1024 * 1024)).toFixed(2)}MB`;
}

function clearFile() {
  file.value = null;
  fieldErrors.value.file = '';
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
}

function onFileChange(event) {
  const selected = event.target.files?.[0] || null;
  file.value = selected;
  fieldErrors.value.file = '';

  if (!selected) {
    return;
  }

  const ext = selected.name.includes('.') ? selected.name.split('.').pop().toLowerCase() : '';
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    fieldErrors.value.file = '附件格式仅支持 PDF/JPG/PNG';
    return;
  }

  if (selected.size > MAX_FILE_SIZE) {
    fieldErrors.value.file = '附件大小不能超过 5MB';
  }
}

async function submitForm() {
  error.value = '';
  success.value = '';

  if (!validateForm()) {
    error.value = '请先修正表单中的错误后再提交';
    return;
  }

  submitting.value = true;

  try {
    const path = type.value === 'MORAL'
      ? '/api/student/declarations/moral'
      : '/api/student/declarations/skill';

    const form = new FormData();
    form.append('term', term.value);
    if (itemCode.value && itemCode.value !== 'OTHER') {
      form.append('itemCode', itemCode.value);
    }
    form.append('itemName', itemName.value);
    form.append('points', String(points.value));
    form.append('description', description.value);
    if (file.value) {
      form.append('attachment', file.value);
    }

    const res = await request(path, { method: 'POST', data: form, isForm: true });
    success.value = res.message || '提交成功';
    if (itemOptions.value.length > 0) {
      itemCode.value = itemOptions.value[0].itemCode;
      itemName.value = itemOptions.value[0].itemName;
    } else {
      itemCode.value = '';
      itemName.value = '';
    }
    points.value = 1;
    description.value = '';
    clearFile();
  } catch (e) {
    error.value = e.message || '提交失败';
  } finally {
    submitting.value = false;
  }
}

async function loadItemOptions() {
  try {
    const res = await request(`/api/student/declaration-items?type=${encodeURIComponent(type.value)}`);
    itemOptions.value = res.data || [];
    if (itemOptions.value.length > 0) {
      itemCode.value = itemOptions.value[0].itemCode;
      itemName.value = itemOptions.value[0].itemName;
    } else {
      itemCode.value = '';
      itemName.value = '';
    }
  } catch (e) {
    itemOptions.value = [];
    itemCode.value = '';
    itemName.value = '';
    error.value = e.message || '加载项目字典失败';
  }
}

watch(type, async () => {
  await loadItemOptions();
});

watch(itemCode, (code) => {
  if (!code || code === 'OTHER') {
    return;
  }
  const selected = itemOptions.value.find((item) => item.itemCode === code);
  if (selected) {
    itemName.value = selected.itemName;
  }
});

onMounted(loadItemOptions);
</script>
