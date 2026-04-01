<template>
  <div class="page-shell">
    <div class="card">
      <h2>个人信息</h2>
      <p>姓名：{{ user?.name }}</p>
      <p>账号：{{ user?.username }}</p>
      <p>角色：{{ user?.role }}</p>
      <p v-if="user?.className">班级：{{ user.className }}</p>
      <p v-if="user?.department">部门：{{ user.department }}</p>
    </div>

    <div class="card">
      <h3>账号安全</h3>
      <p class="muted">请从右上角账号下拉菜单中点击“修改密码”。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useAuth } from '../lib/auth';

const auth = useAuth();
const user = computed(() => auth.state.user);
</script>
