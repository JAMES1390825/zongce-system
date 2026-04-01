import { createRouter, createWebHistory } from 'vue-router';
import { ROLE } from '../lib/roles';
import { authState, initAuth } from '../lib/auth';

import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import NotificationsView from '../views/NotificationsView.vue';

import AdminStudentsView from '../views/admin/StudentsView.vue';
import AdminTeachersView from '../views/admin/TeachersView.vue';
import AdminScoresView from '../views/admin/ScoresView.vue';
import AdminOrgView from '../views/admin/OrgView.vue';
import AdminAccountsView from '../views/admin/AccountsView.vue';
import AdminRulesView from '../views/admin/RulesView.vue';
import AdminImportBatchesView from '../views/admin/ImportBatchesView.vue';
import AdminLogsView from '../views/admin/LogsView.vue';

import TeacherImportView from '../views/teacher/ScoreImportView.vue';
import TeacherEditView from '../views/teacher/ScoreEditView.vue';
import TeacherListView from '../views/teacher/ScoreListView.vue';

import CounselorReviewsView from '../views/counselor/ReviewsView.vue';
import CounselorClassSummaryView from '../views/counselor/ClassSummaryView.vue';

import StudentDeclareView from '../views/student/DeclareView.vue';
import StudentHistoryView from '../views/student/HistoryView.vue';
import StudentScoresView from '../views/student/ScoresView.vue';

const routes = [
  { path: '/login', component: LoginView, meta: { public: true, title: '登录' } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: DashboardView, meta: { title: '首页' } },
  { path: '/profile', redirect: '/dashboard' },
  { path: '/notifications', component: NotificationsView, meta: { title: '通知中心' } },

  { path: '/admin/students', component: AdminStudentsView, meta: { title: '学生管理', roles: [ROLE.ADMIN] } },
  { path: '/admin/teachers', component: AdminTeachersView, meta: { title: '老师管理', roles: [ROLE.ADMIN] } },
  { path: '/admin/org', component: AdminOrgView, meta: { title: '组织管理', roles: [ROLE.ADMIN] } },
  { path: '/admin/accounts', component: AdminAccountsView, meta: { title: '账号管理', roles: [ROLE.ADMIN] } },
  { path: '/admin/rules', component: AdminRulesView, meta: { title: '规则配置', roles: [ROLE.ADMIN] } },
  { path: '/admin/scores', component: AdminScoresView, meta: { title: '全局成绩', roles: [ROLE.ADMIN] } },
  { path: '/admin/import-batches', component: AdminImportBatchesView, meta: { title: '导入批次', roles: [ROLE.ADMIN] } },
  { path: '/admin/logs', component: AdminLogsView, meta: { title: '系统日志', roles: [ROLE.ADMIN] } },

  { path: '/teacher/pe/import', component: TeacherImportView, meta: { title: '体育导入', roles: [ROLE.TEACHER_PE], kind: 'pe' } },
  { path: '/teacher/pe/edit', component: TeacherEditView, meta: { title: '体育录入', roles: [ROLE.TEACHER_PE], kind: 'pe' } },
  { path: '/teacher/pe/list', component: TeacherListView, meta: { title: '体育查询', roles: [ROLE.TEACHER_PE], kind: 'pe' } },

  { path: '/teacher/study/import', component: TeacherImportView, meta: { title: '智育导入', roles: [ROLE.TEACHER_STUDY], kind: 'study' } },
  { path: '/teacher/study/edit', component: TeacherEditView, meta: { title: '智育录入', roles: [ROLE.TEACHER_STUDY], kind: 'study' } },
  { path: '/teacher/study/list', component: TeacherListView, meta: { title: '智育查询', roles: [ROLE.TEACHER_STUDY], kind: 'study' } },

  { path: '/counselor/reviews', component: CounselorReviewsView, meta: { title: '全部审核', roles: [ROLE.COUNSELOR], type: '' } },
  { path: '/counselor/review/moral', component: CounselorReviewsView, meta: { title: '德育审核', roles: [ROLE.COUNSELOR], type: 'MORAL' } },
  { path: '/counselor/review/skill', component: CounselorReviewsView, meta: { title: '技能审核', roles: [ROLE.COUNSELOR], type: 'SKILL' } },
  { path: '/counselor/class-summary', component: CounselorClassSummaryView, meta: { title: '班级汇总', roles: [ROLE.COUNSELOR] } },

  { path: '/student/declare/moral', component: StudentDeclareView, meta: { title: '德育申报', roles: [ROLE.STUDENT], type: 'MORAL' } },
  { path: '/student/declare/skill', component: StudentDeclareView, meta: { title: '技能申报', roles: [ROLE.STUDENT], type: 'SKILL' } },
  { path: '/student/declare', redirect: '/student/declare/moral' },
  { path: '/student/declare/history', component: StudentHistoryView, meta: { title: '我的申报', roles: [ROLE.STUDENT] } },
  { path: '/student/my-score', component: StudentScoresView, meta: { title: '我的成绩', roles: [ROLE.STUDENT] } },

  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  await initAuth();

  if (to.meta.public) {
    if (authState.user && to.path === '/login') {
      return '/dashboard';
    }
    return true;
  }

  if (!authState.user) {
    return '/login';
  }

  const roles = to.meta.roles;
  if (Array.isArray(roles) && roles.length > 0 && !roles.includes(authState.user.role)) {
    return '/dashboard';
  }

  return true;
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 综测系统` : '综测系统';
});

export default router;
