<template>
  <div id="basic-layout">
    <el-container>
      <el-aside :style="asideStyle">
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
    asideStyle.value.width = "auto";
  } else {
    asideStyle.value.width = "200px";
  }
};
</script>

<style scoped lang="less">
#basic-layout {
  margin: 10px;
  height: calc(100vh - 20px);
  overflow: hidden;
}

.el-container {
  height: 100%;
  min-height: 0;
}

.main-content {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  margin-left: 10px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 20px;
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
</style>
