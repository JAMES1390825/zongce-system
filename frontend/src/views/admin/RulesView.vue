<template>
  <div>
    <div class="card">
      <h2>评分规则配置</h2>
      <p class="muted">
        当前计算公式：总分 = 智育 × 权重 + 体育 × 权重 + min(德育, 上限) + min(技能, 上限)
      </p>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div v-if="success" class="alert alert-success">{{ success }}</div>
    </div>

    <div class="card">
      <form @submit.prevent="saveRule">
        <div class="grid">
          <div>
            <label>规则名称</label>
            <input v-model.trim="form.ruleName" placeholder="如 2026春季默认规则" :class="{ 'input-error': fieldErrors.ruleName }" />
            <p v-if="fieldErrors.ruleName" class="field-error">{{ fieldErrors.ruleName }}</p>
          </div>
          <div>
            <label>智育权重（0-1）</label>
            <input v-model.number="form.studyWeight" type="number" min="0" max="1" step="0.01" required :class="{ 'input-error': fieldErrors.studyWeight }" />
            <p v-if="fieldErrors.studyWeight" class="field-error">{{ fieldErrors.studyWeight }}</p>
          </div>
          <div>
            <label>体育权重（0-1）</label>
            <input v-model.number="form.peWeight" type="number" min="0" max="1" step="0.01" required :class="{ 'input-error': fieldErrors.peWeight }" />
            <p v-if="fieldErrors.peWeight" class="field-error">{{ fieldErrors.peWeight }}</p>
          </div>
          <div>
            <label>德育上限</label>
            <input v-model.number="form.moralCap" type="number" min="0" step="0.5" required :class="{ 'input-error': fieldErrors.moralCap }" />
            <p v-if="fieldErrors.moralCap" class="field-error">{{ fieldErrors.moralCap }}</p>
          </div>
          <div>
            <label>技能上限</label>
            <input v-model.number="form.skillCap" type="number" min="0" step="0.5" required :class="{ 'input-error': fieldErrors.skillCap }" />
            <p v-if="fieldErrors.skillCap" class="field-error">{{ fieldErrors.skillCap }}</p>
          </div>
          <div>
            <label>规则说明</label>
            <input v-model.trim="form.remark" placeholder="可选备注" />
          </div>
        </div>
        <button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存规则' }}</button>
      </form>
    </div>

    <div class="card">
      <h3>规则预览</h3>
      <p>
        智育 * {{ format(form.studyWeight) }} + 体育 * {{ format(form.peWeight) }} +
        min(德育, {{ format(form.moralCap) }}) + min(技能, {{ format(form.skillCap) }})
      </p>
      <p class="muted">
        智育+体育权重和：{{ format(weightSum.toFixed(2)) }}
      </p>
      <p v-if="Math.abs(weightSum - 1) > 0.0001" class="field-error">建议智育+体育权重和为 1.00，当前为 {{ format(weightSum.toFixed(2)) }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../lib/http';

const error = ref('');
const success = ref('');
const saving = ref(false);

const form = reactive({
  ruleName: '',
  studyWeight: 0.6,
  peWeight: 0.2,
  moralCap: 20,
  skillCap: 20,
  remark: ''
});

const fieldErrors = ref({
  ruleName: '',
  studyWeight: '',
  peWeight: '',
  moralCap: '',
  skillCap: ''
});

const weightSum = computed(() => Number(form.studyWeight || 0) + Number(form.peWeight || 0));

function setMessage(type, text) {
  if (type === 'error') {
    error.value = text;
    success.value = '';
  } else {
    success.value = text;
    error.value = '';
  }
}

function format(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const n = Number(value);
  return Number.isNaN(n) ? String(value) : n.toFixed(2);
}

function clearFieldErrors() {
  fieldErrors.value = {
    ruleName: '',
    studyWeight: '',
    peWeight: '',
    moralCap: '',
    skillCap: ''
  };
}

function validateForm() {
  clearFieldErrors();

  if (!form.ruleName || form.ruleName.length < 2) {
    fieldErrors.value.ruleName = '规则名称至少 2 个字符';
  }

  const studyWeight = Number(form.studyWeight);
  const peWeight = Number(form.peWeight);
  const moralCap = Number(form.moralCap);
  const skillCap = Number(form.skillCap);

  if (!Number.isFinite(studyWeight) || studyWeight < 0 || studyWeight > 1) {
    fieldErrors.value.studyWeight = '智育权重需在 0 到 1 之间';
  }

  if (!Number.isFinite(peWeight) || peWeight < 0 || peWeight > 1) {
    fieldErrors.value.peWeight = '体育权重需在 0 到 1 之间';
  }

  if (!Number.isFinite(moralCap) || moralCap < 0) {
    fieldErrors.value.moralCap = '德育上限必须大于等于 0';
  }

  if (!Number.isFinite(skillCap) || skillCap < 0) {
    fieldErrors.value.skillCap = '技能上限必须大于等于 0';
  }

  return !fieldErrors.value.ruleName && !fieldErrors.value.studyWeight && !fieldErrors.value.peWeight && !fieldErrors.value.moralCap && !fieldErrors.value.skillCap;
}

async function loadRule() {
  try {
    const res = await request('/api/admin/rules');
    const rule = res.data || {};
    form.ruleName = rule.ruleName || '';
    form.studyWeight = Number(rule.studyWeight ?? 0.6);
    form.peWeight = Number(rule.peWeight ?? 0.2);
    form.moralCap = Number(rule.moralCap ?? 20);
    form.skillCap = Number(rule.skillCap ?? 20);
    form.remark = rule.remark || '';
  } catch (e) {
    setMessage('error', e.message || '加载失败');
  }
}

async function saveRule() {
  if (!validateForm()) {
    setMessage('error', '请先修正表单中的错误后再保存');
    return;
  }

  if (!window.confirm('确认保存规则配置吗？保存后会影响后续重算结果。')) {
    return;
  }

  try {
    saving.value = true;
    const res = await request('/api/admin/rules', {
      method: 'POST',
      data: {
        ruleName: form.ruleName,
        studyWeight: form.studyWeight,
        peWeight: form.peWeight,
        moralCap: form.moralCap,
        skillCap: form.skillCap,
        remark: form.remark
      }
    });
    setMessage('success', res.message || '保存成功');
    await loadRule();
  } catch (e) {
    setMessage('error', e.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(loadRule);
</script>
