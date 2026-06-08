<template>
  <div id="basic-aside">
    <el-menu
      :default-active="defaultPath"
      class="aside-menu"
      :collapse="isCollapse"
    >
      <div class="menu-header">
        <h1 class="logo-text">LSS-RAG-AI</h1>
        <el-text style="color: var(--apple-text-secondary)" size="small">知识库AI问答系统</el-text>
      </div>
      <el-divider />

      <el-menu-item
        v-for="item in menuRouterList"
        :key="item.path"
        :index="item.path"
        @click="handleSelect(item)"
      >
        <el-icon>
          <component :is="item.meta?.icon"></component>
        </el-icon>
        <template #title>{{ item.meta?.description }}</template>
      </el-menu-item>

    </el-menu>
  </div>
</template>

<script setup lang="ts">
import routes from "@/router/config.ts";
import router from "@/router";

const emit = defineEmits(["changeAside"]);
const isCollapse = ref(false);
const path = router.currentRoute.value.fullPath;
const defaultPath = ref(path === "/" ? "/chat" : path);

// 使用计算属性过滤不是菜单项的路由选项
const menuRouterList = computed(() => {
  const userRole = localStorage.getItem("userRole");
  return routes.filter((item) => {
    if (!item.meta?.isMenu) {
      return false;
    }
    if (item.meta?.roles && Array.isArray(item.meta.roles)) {
      return !!userRole && item.meta.roles.includes(userRole);
    }
    return true;
  });
});

router.afterEach((to) => {
  defaultPath.value = to.path;
});

onMounted(() => {
  console.log(defaultPath.value);
});

const handleSelect = (e: any) => {
  console.log(e);
  router.push({
    path: e.path,
  });
};
</script>

<style scoped lang="less">
#basic-aside {
  height: 100%;
}

:deep(.el-menu) {
  z-index: 10;
}

.menu-header {
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  padding: 10px 0;
}

.logo-text {
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 50%, var(--apple-indigo) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.aside-menu {
  height: 100%;
  border-radius: var(--radius-lg);
  border: 1px solid var(--apple-border);
  background: var(--apple-card);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  padding: 8px;

  :deep(.el-menu-item) {
    border-radius: var(--radius-sm);
    margin-bottom: 4px;
    height: 44px;
    line-height: 44px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    color: var(--apple-text-primary);

    &:hover {
      background: rgba(0, 122, 255, 0.08) !important;
      transform: translateX(4px);
    }

    &.is-active {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.12) 0%, rgba(175, 82, 222, 0.12) 100%) !important;
      color: var(--apple-blue) !important;
      font-weight: 600;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 20px;
        background: linear-gradient(180deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
        border-radius: 0 3px 3px 0;
      }
    }
  }

  :deep(.el-divider) {
    margin: 10px 0;
    --el-divider-border-color: var(--apple-border);
  }
}
</style>
