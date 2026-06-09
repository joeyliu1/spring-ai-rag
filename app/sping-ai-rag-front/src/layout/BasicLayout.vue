<template>
  <div id="basic-layout">
    <el-container class="layout-shell">
      <el-aside :style="asideStyle" class="layout-aside">
        <BasicAside @change-aside="changeAside" />
      </el-aside>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
const asideStyle = ref({
  height: "95vh",
  width: "210px",
  transition: "width 0.4s cubic-bezier(0.4, 0, 0.2, 1)",
});
const changeAside = (isCollapse: boolean) => {
  if (isCollapse) {
    asideStyle.value.width = "96px";
  } else {
    asideStyle.value.width = "236px";
  }
};
</script>

<style scoped lang="less">
#basic-layout {
  padding: 16px;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
}

.layout-shell {
  height: 100%;
  min-height: 0;
  gap: 14px;
}

.main-content {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  padding: 18px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: var(--shadow-md);
  overflow-y: auto;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}

.layout-aside {
  height: 100%;
}
</style>
