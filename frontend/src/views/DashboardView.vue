<template>
  <div class="page-shell">
    <section class="page-header">
      <div>
        <p class="page-eyebrow">校园综测平台</p>
        <h2 class="page-title">欢迎回来，{{ user?.name || '-' }}</h2>
        <p class="page-desc">
          你可以从下方入口进入日常工作页面。
        </p>
      </div>
    </section>

    <section class="shortcut-grid">
      <article class="card shortcut-card" v-for="item in shortcuts" :key="item.to">
        <h3>{{ item.title }}</h3>
        <p class="muted">{{ item.desc }}</p>
        <div class="shortcut-footer">
          <RouterLink class="ghost-link" :to="item.to">进入页面</RouterLink>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { useAuth } from '../lib/auth';
import { ROLE } from '../lib/roles';

const auth = useAuth();
const user = computed(() => auth.state.user);

const shortcutMap = {
  [ROLE.ADMIN]: [
    { title: '学生管理', desc: '新增/编辑/删除学生账号', to: '/admin/students' },
    { title: '老师管理', desc: '维护老师与辅导员账号', to: '/admin/teachers' },
    { title: '全局成绩', desc: '按班级和学期查询全局成绩', to: '/admin/scores' }
  ],
  [ROLE.TEACHER_PE]: [
    { title: '体育导入', desc: 'CSV 批量导入体育成绩', to: '/teacher/pe/import' },
    { title: '体育录入', desc: '单条补录体育分项成绩', to: '/teacher/pe/edit' },
    { title: '体育查询/修改', desc: '先查询成绩，再在列表中点击编辑修改单项', to: '/teacher/pe/list' }
  ],
  [ROLE.TEACHER_STUDY]: [
    { title: '智育导入', desc: 'CSV 批量导入智育成绩', to: '/teacher/study/import' },
    { title: '智育录入', desc: '单条补录智育科目成绩', to: '/teacher/study/edit' },
    { title: '智育查询/修改', desc: '先查询成绩，再在列表中点击编辑修改单项', to: '/teacher/study/list' }
  ],
  [ROLE.COUNSELOR]: [
    { title: '德育审核', desc: '审核学生德育申报', to: '/counselor/review/moral' },
    { title: '技能审核', desc: '审核学生技能申报', to: '/counselor/review/skill' },
    { title: '班级汇总', desc: '重算并查看班级综测排名', to: '/counselor/class-summary' }
  ],
  [ROLE.STUDENT]: [
    { title: '德育申报', desc: '提交德育加分申报', to: '/student/declare/moral' },
    { title: '技能申报', desc: '提交技能加分申报', to: '/student/declare/skill' },
    { title: '我的成绩', desc: '查看综测分与班级排名', to: '/student/my-score' }
  ]
};

const shortcuts = computed(() => {
  const role = user.value?.role;
  return shortcutMap[role] || [];
});
</script>
