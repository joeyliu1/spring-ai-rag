<template>
  <div id="basic-aside">
    <el-menu
      :default-active="defaultPath"
      class="aside-menu"
      :collapse="isCollapse"
    >
      <div class="menu-header">
        <div class="brand-mark">L</div>
        <div class="brand-copy">
          <h1 class="logo-text">LSS-RAG-AI</h1>
          <el-text style="color: var(--apple-text-secondary)" size="small">知识库 AI 问答系统</el-text>
        </div>
      </div>
      <el-divider />

      <div class="menu-group">
        <div class="menu-group-label">工作区</div>
        <el-menu-item
          v-for="item in workspaceRouterList"
          :key="item.path"
          :index="item.path"
          @click="handleSelect(item)"
        >
          <el-icon>
            <component :is="item.meta?.icon"></component>
          </el-icon>
          <template #title>{{ item.meta?.description }}</template>
        </el-menu-item>
      </div>

      <div v-if="managementRouterList.length > 0" class="menu-group">
        <div class="menu-group-label">管理</div>
        <el-menu-item
          v-for="item in managementRouterList"
          :key="item.path"
          :index="item.path"
          @click="handleSelect(item)"
        >
          <el-icon>
            <component :is="item.meta?.icon"></component>
          </el-icon>
          <template #title>{{ item.meta?.description }}</template>
        </el-menu-item>
      </div>

    </el-menu>
  </div>
</template>

<script setup lang="ts">
import routes from "@/router/config.ts";
import router from "@/router";

const emit = defineEmits(["changeAside"]);
const isCollapse = ref(false);
const path = router.currentRoute.value.fullPath;
const defaultPath = ref(path === "/" ? "/ragChat" : path);

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

const workspaceRouterList = computed(() => {
  return menuRouterList.value.filter((item) => !item.meta?.roles || item.meta.roles.length === 0);
});

const managementRouterList = computed(() => {
  return menuRouterList.value.filter((item) => Array.isArray(item.meta?.roles) && item.meta.roles.length > 0);
});

router.afterEach((to) => {
  defaultPath.value = to.path;
});

onMounted(() => {
  emit("changeAside", isCollapse.value);
});

const handleSelect = (e: any) => {
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
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px 8px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
  box-shadow: 0 10px 24px rgba(0, 122, 255, 0.24);
}

.brand-copy {
  min-width: 0;
}

.logo-text {
  margin: 0;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 50%, var(--apple-indigo) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.1;
}

.aside-menu {
  height: 100%;
  border-radius: var(--radius-xl);
  border: 1px solid var(--apple-border);
  background: var(--apple-card);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  padding: 10px 8px;
}

.menu-group {
  margin-top: 6px;
}

.menu-group-label {
  margin: 10px 10px 8px;
  color: var(--apple-text-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.aside-menu {
  .menu-group-label {
    margin: 10px 10px 8px;
    color: var(--apple-text-secondary);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0;
  }

  :deep(.el-menu-item) {
    border-radius: 12px;
    margin-bottom: 6px;
    height: 44px;
    line-height: 44px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    color: var(--apple-text-primary);
    overflow: hidden;

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

:deep(.el-menu--collapse) {
  padding-left: 6px;
  padding-right: 6px;
}

:deep(.el-menu--collapse .menu-header) {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

:deep(.el-menu--collapse .brand-copy),
:deep(.el-menu--collapse .menu-group-label),
:deep(.el-menu--collapse .el-divider) {
  display: none;
}
</style>
