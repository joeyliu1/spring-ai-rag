<template>
  <div class="know-hub-page">
    <div class="page-header">
      <div>
        <h2>我的知识库</h2>
        <p>上传、分块、索引和管理放在同一工作区里，少跳转，少找入口。</p>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <strong>{{ storeFileTotal }}</strong>
          <span>知识文件</span>
        </div>
        <div class="stat-card">
          <strong>{{ selectedFiles.length }}</strong>
          <span>已选文件</span>
        </div>
      </div>
    </div>

    <div class="top-grid">
      <!-- Upload Area -->
      <div class="panel upload-section">
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

      <!-- Chunk Strategy -->
      <div class="panel chunk-config-section">
        <div class="chunk-config-header">
          <div>
            <div class="chunk-config-title">
              <el-icon><Setting /></el-icon>
              分块策略
            </div>
            <div class="chunk-config-meta">
              递归分块：段落 / 换行 / 中英文标点 / 空格，保留相邻块重叠上下文
            </div>
          </div>
          <el-button size="small" @click="resetChunkConfig" :disabled="isChunkConfigLoading">
            恢复默认
          </el-button>
        </div>
        <div class="chunk-config-grid" v-loading="isChunkConfigLoading">
          <div class="chunk-config-item">
            <span>目标块大小</span>
            <el-input-number
              v-model="chunkConfig.chunkSize"
              :min="chunkConfig.minAllowedChunkSize || 200"
              :max="chunkConfig.maxAllowedChunkSize || 4000"
              :step="100"
              controls-position="right"
            />
          </div>
          <div class="chunk-config-item">
            <span>重叠大小</span>
            <el-input-number
              v-model="chunkConfig.overlapSize"
              :min="0"
              :max="Math.floor(chunkConfig.chunkSize / 2)"
              :step="50"
              controls-position="right"
            />
          </div>
          <div class="chunk-config-item">
            <span>最小块大小</span>
            <el-input-number
              v-model="chunkConfig.minChunkSize"
              :min="1"
              :max="chunkConfig.chunkSize"
              :step="20"
              controls-position="right"
            />
          </div>
          <div class="chunk-config-item">
            <span>最大分块数</span>
            <el-input-number
              v-model="chunkConfig.maxChunks"
              :min="1"
              :max="chunkConfig.maxAllowedChunks || 20000"
              :step="500"
              controls-position="right"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar panel">
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
    <div class="table-section panel">
      <el-table
        :data="storeFileData"
        border
        v-loading="isLoading"
        height="100%"
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
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                @click="openFileDetail(scope.row)"
                type="info"
                size="small"
              >
                详情
              </el-button>
              <el-button
                @click="openChunkPreview(scope.row)"
                type="success"
                size="small"
              >
                分块
              </el-button>
              <el-button
                @click="rebuildFileIndex(scope.row)"
                type="warning"
                size="small"
                :loading="rebuildingFileId === scope.row.id"
              >
                重建
              </el-button>
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

    <el-dialog
      v-model="detailDialogVisible"
      title="文件详情"
      width="720px"
    >
      <div v-if="currentFileDetail" class="detail-grid">
        <div class="detail-item">
          <span>文件名</span>
          <strong>{{ currentFileDetail.file.fileName }}</strong>
        </div>
        <div class="detail-item">
          <span>文件 ID</span>
          <strong>{{ currentFileDetail.file.id }}</strong>
        </div>
        <div class="detail-item">
          <span>分块数量</span>
          <strong>{{ currentFileDetail.chunkCount }}</strong>
        </div>
        <div class="detail-item">
          <span>向量数量</span>
          <strong>{{ currentFileDetail.vectorCount }}</strong>
        </div>
        <div class="detail-item">
          <span>上传时间</span>
          <strong>{{ format(new Date(currentFileDetail.file.createTime), "yyyy-MM-dd HH:mm") }}</strong>
        </div>
        <div class="detail-item">
          <span>更新时间</span>
          <strong>{{ format(new Date(currentFileDetail.file.updateTime), "yyyy-MM-dd HH:mm") }}</strong>
        </div>
      </div>
      <el-input
        v-if="currentFileDetail?.vectorIds?.length"
        :model-value="currentFileDetail.vectorIds.join('\n')"
        type="textarea"
        :rows="6"
        readonly
        class="vector-list"
      />
    </el-dialog>

    <el-dialog
      v-model="chunkDialogVisible"
      title="分块预览"
      width="960px"
      top="5vh"
    >
      <el-table
        :data="chunkData"
        border
        v-loading="isChunkLoading"
        max-height="520"
      >
        <el-table-column prop="chunkIndex" label="Index" width="80" />
        <el-table-column prop="chunkSize" label="大小" width="90" />
        <el-table-column prop="documentId" label="向量 ID" width="220" show-overflow-tooltip />
        <el-table-column label="内容预览" min-width="420">
          <template #default="scope">
            <div class="chunk-preview">
              <div class="chunk-preview-text">{{ scope.row.preview }}</div>
              <el-collapse>
                <el-collapse-item title="查看完整内容">
                  <pre>{{ scope.row.content }}</pre>
                </el-collapse-item>
              </el-collapse>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="chunkQuery.page"
          v-model:page-size="chunkQuery.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="chunkTotal"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleChunkSizeChange"
          @current-change="handleChunkCurrentChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { type UploadUserFile, ElMessage, ElMessageBox } from "element-plus";
import { UploadFilled, Delete, Download, Upload, Search, Setting } from '@element-plus/icons-vue'
import {
  uploadFileApi,
  queryFileApi,
  deleteFileApi,
  downloadFileApi,
  queryChunkConfigApi,
  queryFileDetailApi,
  queryFileChunksApi,
  rebuildFileIndexApi
} from "@/api/KnowHubApi";
import { KnowledgeChunk, KnowledgeFileDetail, StoreFile } from "@/api/data";
import { ChunkConfig, QueryFileDto } from "@/api/dto";
import { format } from "date-fns";

const storeFileData = ref<StoreFile[]>([]);
const queryFileDto = ref<QueryFileDto>({
  page: 1,
  pageSize: 10,
  fileName: "",
});
const isUploading = ref(false);
const isLoading = ref(false);
const isChunkConfigLoading = ref(false);
const storeFileTotal = ref(0);
const selectedFiles = ref<any[]>([])
const currentFileId = ref<number | null>(null)
const currentFileDetail = ref<KnowledgeFileDetail | null>(null)
const detailDialogVisible = ref(false)
const chunkDialogVisible = ref(false)
const isChunkLoading = ref(false)
const chunkData = ref<KnowledgeChunk[]>([])
const chunkTotal = ref(0)
const chunkQuery = ref({ page: 1, pageSize: 10 })
const rebuildingFileId = ref<number | null>(null)
const defaultChunkConfig = ref<ChunkConfig>({
  chunkSize: 1200,
  overlapSize: 200,
  minChunkSize: 80,
  maxChunks: 10000,
  minAllowedChunkSize: 200,
  maxAllowedChunkSize: 4000,
  maxAllowedOverlapSize: 600,
  maxAllowedChunks: 20000,
});
const chunkConfig = ref<ChunkConfig>({ ...defaultChunkConfig.value });

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

const loadChunkConfig = () => {
  isChunkConfigLoading.value = true;
  queryChunkConfigApi()
    .then((res) => {
      if (res.code === 0) {
        defaultChunkConfig.value = res.data;
        chunkConfig.value = { ...res.data };
      } else {
        ElMessage({ type: "error", message: res.message });
      }
    })
    .catch((err) => {
      ElMessage({ type: "error", message: err });
    })
    .finally(() => {
      isChunkConfigLoading.value = false;
    });
};

const resetChunkConfig = () => {
  chunkConfig.value = { ...defaultChunkConfig.value };
};

const normalizeChunkConfig = () => {
  const maxOverlap = Math.floor(chunkConfig.value.chunkSize / 2);
  if (chunkConfig.value.overlapSize > maxOverlap) {
    chunkConfig.value.overlapSize = maxOverlap;
  }
  if (chunkConfig.value.minChunkSize > chunkConfig.value.chunkSize) {
    chunkConfig.value.minChunkSize = chunkConfig.value.chunkSize;
  }
};

const fileList = ref<UploadUserFile[]>([]);

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

  if (files.length === 0) {
    ElMessage({
      type: "warning",
      message: "请先选择文件",
    });
    return;
  }

  normalizeChunkConfig();
  isUploading.value = true;
  uploadFileApi(files, {
    chunkSize: chunkConfig.value.chunkSize,
    overlapSize: chunkConfig.value.overlapSize,
    minChunkSize: chunkConfig.value.minChunkSize,
    maxChunks: chunkConfig.value.maxChunks,
  })
    .then((res) => {
      let code = res.data.code;
      if (code == 0) {
        const result = res.data.data;
        const summary = result?.totalChunkCount
          ? `上传成功，共 ${result.fileCount} 个文件，生成 ${result.totalChunkCount} 个分块`
          : "文件上传成功";
        ElMessage({
          type: "success",
          message: summary,
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

const openFileDetail = async (file: StoreFile) => {
  try {
    const res = await queryFileDetailApi(file.id)
    if (res.code === 0) {
      currentFileDetail.value = res.data
      detailDialogVisible.value = true
    } else {
      ElMessage({
        type: "error",
        message: res.message,
      });
    }
  } catch (err) {
    ElMessage({
      type: "error",
      message: String(err),
    });
  }
}

const openChunkPreview = async (file: StoreFile) => {
  currentFileId.value = file.id
  chunkQuery.value.page = 1
  chunkDialogVisible.value = true
  await loadFileChunks()
}

const loadFileChunks = async () => {
  if (!currentFileId.value) return
  isChunkLoading.value = true
  try {
    const res = await queryFileChunksApi(currentFileId.value, chunkQuery.value)
    if (res.code === 0) {
      chunkData.value = res.data.records || []
      chunkTotal.value = res.data.total || 0
    } else {
      ElMessage({
        type: "error",
        message: res.message,
      });
    }
  } catch (err) {
    ElMessage({
      type: "error",
      message: String(err),
    });
  } finally {
    isChunkLoading.value = false
  }
}

const rebuildFileIndex = (file: StoreFile) => {
  ElMessageBox.confirm(`确定要重建 ${file.fileName} 的向量索引吗？`, "重建索引", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      rebuildingFileId.value = file.id
      try {
        const res = await rebuildFileIndexApi(file.id)
        if (res.code === 0) {
          ElMessage({
            type: "success",
            message: res.data,
          });
          loadStoreFileData()
        } else {
          ElMessage({
            type: "error",
            message: res.message,
          });
        }
      } catch (err) {
        ElMessage({
          type: "error",
          message: String(err),
        });
      } finally {
        rebuildingFileId.value = null
      }
    })
    .catch(() => {});
}

const handleChunkSizeChange = (val: number) => {
  chunkQuery.value.pageSize = val
  chunkQuery.value.page = 1
  loadFileChunks()
}

const handleChunkCurrentChange = (val: number) => {
  chunkQuery.value.page = val
  loadFileChunks()
}

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
  loadChunkConfig();
  loadStoreFileData();
});
</script>

<style scoped lang="less">
.know-hub-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;

  h2 {
    margin: 0;
    font-size: 22px;
    color: var(--apple-text-primary);
  }

  p {
    margin: 6px 0 0;
    color: var(--apple-text-secondary);
  }
}

.header-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-card {
  min-width: 108px;
  padding: 12px 14px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-sm);

  strong {
    display: block;
    font-size: 20px;
    color: var(--apple-text-primary);
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
}

.panel {
  padding: 16px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: var(--shadow-sm);
  min-width: 0;
}

.upload-section {
  display: flex;
  flex-direction: column;

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

.chunk-config-section {
  .chunk-config-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
  }

  .chunk-config-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--apple-text-primary);
    font-size: 15px;
    font-weight: 600;

    .el-icon {
      color: var(--apple-blue);
    }
  }

  .chunk-config-meta {
    margin-top: 4px;
    color: var(--apple-text-secondary);
    font-size: 12px;
  }

  .chunk-config-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 14px;
    min-height: 58px;
  }

  .chunk-config-item {
    display: flex;
    flex-direction: column;
    gap: 6px;

    span {
      color: var(--apple-text-secondary);
      font-size: 12px;
    }

    :deep(.el-input-number) {
      width: 100%;
    }
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  min-height: 360px;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;

  :deep(.el-table) {
    flex: 1;
  }

  .action-buttons {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
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

@media (max-width: 1180px) {
  .top-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 860px) {
  .chunk-config-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;

    .toolbar-left {
      flex-wrap: wrap;
    }

    .upload-btn {
      align-self: flex-end;
    }
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  padding: 12px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.02);

  span {
    display: block;
    margin-bottom: 6px;
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  strong {
    display: block;
    color: var(--apple-text-primary);
    font-size: 14px;
    word-break: break-all;
  }
}

.vector-list {
  margin-top: 16px;
}

.chunk-preview-text {
  color: var(--apple-text-primary);
  line-height: 1.6;
}

.chunk-preview pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: var(--apple-text-primary);
  line-height: 1.6;
}

.dialog-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
