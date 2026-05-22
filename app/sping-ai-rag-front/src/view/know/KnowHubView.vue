<template>
  <div class="know-hub-container">
    <!-- Upload Area -->
    <div class="upload-section">
      <el-upload
        class="upload-demo"
        drag
        multiple
        v-model:file-list="fileList"
        :auto-upload="false"
        v-loading="isUploading"
      >
        <div class="upload-content">
          <el-icon class="upload-icon"><upload-filled /></el-icon>
          <div class="upload-text">
            拖拽文件至此或<em>点击选择文件</em>进行上传
          </div>
        </div>
        <template #tip>
          <div class="upload-tip">
            <el-text>文件支持 <i>pdf、doc、md、excel、text</i> 等，最大可上传 <em style="color: var(--apple-blue)">100MB</em></el-text>
          </div>
        </template>
      </el-upload>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button
          type="danger"
          @click="batchDelete"
          :disabled="selectedFiles.length === 0"
        >
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button
          type="primary"
          @click="batchDownload"
          :disabled="selectedFiles.length === 0"
        >
          <el-icon><Download /></el-icon>
          批量下载
        </el-button>
        <div class="search-box">
          <el-input
            placeholder="搜索文件名..."
            v-model="queryFileDto.fileName"
            clearable
            style="width: 200px"
          />
          <el-button type="primary" @click="loadStoreFileData" :disabled="isLoading">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>
      <el-button
        type="warning"
        @click="uploadFile"
        :disabled="isUploading"
        class="upload-btn"
      >
        <el-icon><Upload /></el-icon>
        全部上传
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-section">
      <el-table
        :data="storeFileData"
        border
        v-loading="isLoading"
        height="calc(100vh - 400px)"
        @selection-change="handleSelectionChange"
        class="apple-table"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="序号" width="80">
          <template #default="scope">
            {{ (queryFileDto.page - 1) * queryFileDto.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="400" show-overflow-tooltip />
        <el-table-column label="上传时间" width="180">
          <template #default="scope">
            {{ format(new Date(scope.row.createTime), "yyyy-MM-dd HH:mm") }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="scope">
            {{ format(new Date(scope.row.updateTime), "yyyy-MM-dd HH:mm") }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                @click="deleteStoreFile(scope.row)"
                type="danger"
                size="small"
                :icon="Delete"
              >
                删除
              </el-button>
              <el-button
                @click="openFilePreview(scope.row)"
                type="primary"
                size="small"
                :icon="Download"
              >
                下载
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination -->
    <div class="pagination-section">
      <el-pagination
        v-model:current-page="queryFileDto.page"
        v-model:page-size="queryFileDto.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="storeFileTotal"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { type UploadUserFile, ElMessage, ElMessageBox } from "element-plus";
import { UploadFilled, Delete, Download, Upload, Search } from '@element-plus/icons-vue'
import {uploadFileApi, queryFileApi, deleteFileApi, downloadFileApi} from "@/api/KnowHubApi";
import { StoreFile } from "@/api/data";
import { QueryFileDto } from "@/api/dto";
import { format } from "date-fns";

const storeFileData = ref<StoreFile[]>([]);
const queryFileDto = ref<QueryFileDto>({
  page: 1,
  pageSize: 10,
  fileName: "",
});
const isUploading = ref(false);
const isLoading = ref(false);
const storeFileTotal = ref(0);
const selectedFiles = ref<any[]>([])

const loadStoreFileData = () => {
  isLoading.value = true;
  const params = { ...queryFileDto.value, page: queryFileDto.value.page - 1 }
  queryFileApi(params)
    .then((res) => {
      if (res.code == 0) {
        const data = res.data;
        storeFileTotal.value = data.totalElements;
        storeFileData.value = data.records;
      } else {
        ElMessage({
          type: "error",
          message: res.message,
        });
      }
    })
    .catch((err) => {
      ElMessage({
        type: "error",
        message: err,
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const fileList = ref<UploadUserFile[]>();

const uploadFile = () => {
  const files: File[] = [];
  fileList.value?.forEach((e) => {
    files.push(e.raw as File);
  });

  const maxSize = 100 * 1024 * 1024;
  for (const file of files) {
    if (file.size > maxSize) {
      ElMessage({
        type: "error",
        message: `文件 ${file.name} 超过了最大上传大小限制 (100MB)`,
      });
      return;
    }
  }

  isUploading.value = true;
  uploadFileApi(files)
    .then((res) => {
      let code = res.data.code;
      if (code == 0) {
        ElMessage({
          type: "success",
          message: res.data.data,
        });
        fileList.value = [];
        loadStoreFileData();
      } else {
        ElMessage({
          type: "error",
          message: res.data.message,
        });
      }
    })
    .catch((err) => {
      console.log(err);
      ElMessage({
        type: "error",
        message: err,
      });
    })
    .finally(() => {
      isUploading.value = false;
    });
};

const deleteStoreFile = (e: any) => {
  ElMessageBox.confirm("确定要删除这个知识库吗？", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      deleteFileApi({
        ids: e.id,
      })
        .then((res) => {
          let code = res.code;
          if (code == 0) {
            ElMessage({
              type: "success",
              message: res.data,
            });
            loadStoreFileData();
          } else {
            ElMessage({
              type: "error",
              message: res.message,
            });
          }
        })
        .catch((err) => {
          ElMessage({
            type: "error",
            message: err,
          });
        });
    })
    .catch(() => {});
};

const openFilePreview = (e: any) => {
  downloadFileApi({
    ids: e.id,
  })
    .then((res) => {
      let code = res.code;
      if (code == 0) {
        ElMessage({
          type: "success",
          message: res.data,
        });
        loadStoreFileData();
      } else {
        ElMessage({
          type: "error",
          message: res.message,
        });
      }
    })
    .catch((err) => {
      ElMessage({
        type: "error",
        message: err,
      });
    });
};

const handleSizeChange = (val: number) => {
  queryFileDto.value.pageSize = val
  loadStoreFileData()
}

const handleCurrentChange = (val: number) => {
  queryFileDto.value.page = val
  loadStoreFileData()
}

const handleSelectionChange = (selection: any[]) => {
  selectedFiles.value = selection
}

const batchDelete = () => {
  if (selectedFiles.value.length === 0) return

  ElMessageBox.confirm("确定要删除选中的文件吗？", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      const ids = selectedFiles.value.map(file => file.id).join(',')
      deleteFileApi({
        ids: ids,
      })
        .then((res) => {
          if (res.code == 0) {
            ElMessage({
              type: "success",
              message: res.data,
            });
            selectedFiles.value = [];
            loadStoreFileData();
          } else {
            ElMessage({
              type: "error",
              message: res.message,
            });
          }
        })
        .catch((err) => {
          ElMessage({
            type: "error",
            message: err,
          });
        });
    })
    .catch(() => {});
}

const batchDownload = () => {
  if (selectedFiles.value.length === 0) return

  const ids = selectedFiles.value.map(file => file.id).join(',')
  downloadFileApi({
    ids: ids,
  })
    .then((res) => {
      if (res.code == 0) {
        ElMessage({
          type: "success",
          message: res.data,
        });
        selectedFiles.value = [];
        loadStoreFileData();
      } else {
        ElMessage({
          type: "error",
          message: res.message,
        });
      }
    })
    .catch((err) => {
      ElMessage({
        type: "error",
        message: err,
      });
    });
}

onMounted(() => {
  loadStoreFileData();
});
</script>

<style scoped lang="less">
.know-hub-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-section {
  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    height: auto;
    min-height: 140px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-lg);
    border: 2px dashed var(--apple-border);
    background: rgba(255, 255, 255, 0.5);
    backdrop-filter: blur(10px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: var(--apple-blue);
      background: rgba(0, 122, 255, 0.03);
    }
  }

  .upload-content {
    padding: 20px;
    text-align: center;
  }

  .upload-icon {
    font-size: 48px;
    color: var(--apple-blue);
    margin-bottom: 12px;
  }

  .upload-text {
    color: var(--apple-text-secondary);
    font-size: 14px;

    em {
      color: var(--apple-blue);
      font-style: normal;
      font-weight: 500;
    }
  }

  .upload-tip {
    margin-top: 12px;
    color: var(--apple-text-secondary);
    font-size: 13px;

    i {
      font-style: normal;
      color: var(--apple-text-primary);
    }
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--apple-card);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--apple-border);
  box-shadow: var(--shadow-sm);

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .search-box {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: 12px;
  }

  .upload-btn {
    background: linear-gradient(135deg, var(--apple-orange) 0%, #FFCC00 100%) !important;
    border: none !important;
    box-shadow: 0 4px 12px rgba(255, 149, 0, 0.25) !important;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(255, 149, 0, 0.35) !important;
    }
  }
}

.table-section {
  flex: 1;
  background: var(--apple-card);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--apple-border);
  box-shadow: var(--shadow-sm);
  overflow: hidden;

  .action-buttons {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    width: 100%;

    :deep(.el-button) {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      width: 80px !important;
      min-width: 80px !important;
      max-width: 80px !important;
      height: 28px !important;
      padding: 0 10px !important;
      margin: 0 !important;
      font-size: 13px !important;
      border: none !important;
    }

    :deep(.el-button .el-icon) {
      display: inline-flex;
      align-items: center;
      margin-right: 4px;
      font-size: 14px;
    }
  }

  :deep(.el-table) {
    --el-table-border-color: var(--apple-border);
    --el-table-header-bg-color: rgba(0, 122, 255, 0.04);
    border-radius: var(--radius-lg);
  }

  :deep(.el-table__header th) {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.06) 0%, rgba(175, 82, 222, 0.06) 100%) !important;
    color: var(--apple-text-primary);
    font-weight: 600;
    padding: 14px 12px;
  }

  :deep(.el-table__row) {
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover > td {
      background-color: rgba(0, 122, 255, 0.03) !important;
    }
  }

  :deep(.el-table__cell) {
    padding: 12px;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
    border-color: var(--apple-blue);
  }
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 16px 0;

  :deep(.el-pagination) {
    --el-pagination-button-bg-color: var(--apple-card);
    --el-pagination-button-color: var(--apple-text-primary);
    --el-pagination-hover-color: var(--apple-blue);

    .el-pager li {
      border-radius: var(--radius-sm);
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        background-color: rgba(0, 122, 255, 0.08);
      }

      &.is-active {
        background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%) !important;
        color: #fff !important;
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
      }
    }

    .el-pagination__total {
      color: var(--apple-text-secondary);
    }
  }
}

.el-table {
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  ::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
  }
  ::-webkit-scrollbar-track {
    background: transparent;
  }
}
</style>
