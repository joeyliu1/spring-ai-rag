<template>
  <div class="log-manage">
    <el-card class="box-card">
      <!-- 搜索表单 -->
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" ref="searchFormRef">
          <el-form-item label="方法名" prop="methodName">
            <el-input v-model="searchForm.methodName" placeholder="请输入方法名" clearable />
          </el-form-item>
          <el-form-item label="类名" prop="className">
            <el-input v-model="searchForm.className" placeholder="请输入类名" clearable />
          </el-form-item>
          <el-form-item label="请求参数" prop="requestParams">
            <el-input v-model="searchForm.requestParams" placeholder="请输入请求参数" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 工具栏 -->
      <div class="table-header">
        <el-button class="delete-selected-btn" type="danger" :disabled="selectedIds.length === 0" @click="handleDeleteSelected">
          删除选中
        </el-button>
        <el-button class="clear-all-btn" type="danger" @click="handleClearAll">
          清空全部
        </el-button>
        <span class="selection-count">已选 {{ selectedIds.length }} 条</span>
      </div>
      
      <!-- 表格 -->
      <div class="table-container">
        <el-table
          v-loading="isLoading"
          element-loading-text="加载中..."
          element-loading-background="rgba(255, 255, 255, 0.8)"
          :data="logList" 
          style="width: 100%" 
          border
          height="calc(95vh - 280px)"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="methodName" label="方法名" width="150" show-overflow-tooltip />
          <el-table-column prop="className" label="类名" width="300" show-overflow-tooltip />
          <el-table-column prop="requestTime" label="产生时间" width="180">
            <template #default="scope">
              {{ new Date(scope.row.requestTime).toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="requestParams" label="请求参数" show-overflow-tooltip />
<!--          <el-table-column prop="response" label="响应结果" show-overflow-tooltip />-->
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryLogApi, batchDeleteLogApi, type LogInfo, type LogQueryParams } from '@/api/LogApi'
import type { FormInstance } from 'element-plus'

const logList = ref<LogInfo[]>([])
const total = ref(0)
const isLoading = ref(false)
const selectedIds = ref<string[]>([])
const searchFormRef = ref<FormInstance>()
// 查询参数
const queryParams = ref<LogQueryParams>({
  page: 1,
  size: 10,
  methodName: '',
  className: '',
  requestParams: ''
})

// 搜索表单
const searchForm = ref({
  methodName: '',
  className: '',
  requestParams: ''
})

// 加载日志数据
const loadLogData = async () => {
  isLoading.value = true
  try {
    const params = { 
      ...queryParams.value, 
      page: queryParams.value.page - 1 
    }
    const res = await queryLogApi(params)
    if (res.code === 0) {
      logList.value = res.data.records
      total.value = res.data.total
    } else {
      ElMessage.error(res.message || '获取日志列表失败')
    }
  } catch (error) {
    console.error('获取日志列表错误:', error)
    ElMessage.error('获取日志列表失败')
  } finally {
    isLoading.value = false
  }
}

// 处理多选
const handleSelectionChange = (selection: LogInfo[]) => {
  selectedIds.value = selection.map(item => item.id.toString())
}

// 删除选中
const handleDeleteSelected = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条日志吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const res = await batchDeleteLogApi(selectedIds.value)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        selectedIds.value = []
        loadLogData()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除日志错误:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 清空全部
const handleClearAll = () => {
  ElMessageBox.confirm(
    `确定要清空全部日志吗？该操作不可撤销。`,
    '警告',
    {
      confirmButtonText: '清空全部',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const res = await batchDeleteLogApi()
      if (res.code === 0) {
        ElMessage.success('清空成功')
        selectedIds.value = []
        queryParams.value.page = 1
        loadLogData()
      } else {
        ElMessage.error(res.message || '清空失败')
      }
    } catch (error) {
      console.error('清空日志错误:', error)
      ElMessage.error('清空失败')
    }
  }).catch(() => {
    ElMessage.info('已取消清空')
  })
}

// 搜索
const handleSearch = () => {
  queryParams.value = {
    ...queryParams.value,
    ...searchForm.value,
    page: 1
  }
  loadLogData()
}

// 重置搜索
const resetSearch = () => {
  if (searchFormRef.value) {
    searchFormRef.value.resetFields()
  }
  queryParams.value = {
    page: 1,
    size: 10,
    methodName: '',
    className: '',
    requestParams: ''
  }
  loadLogData()
}

// 分页处理
const handleSizeChange = (val: number) => {
  queryParams.value.size = val
  queryParams.value.page = 1
  loadLogData()
}

const handleCurrentChange = (val: number) => {
  queryParams.value.page = val
  loadLogData()
}

onMounted(() => {
  loadLogData()
})
</script>

<style scoped lang="less">
.log-manage {
  height: 95vh;
  padding: 20px;
  box-sizing: border-box;
  
  .box-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    
    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 20px;
    }
  }

  .search-form {
    margin-bottom: 20px;
  }

  .table-header {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .selection-count {
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  .delete-selected-btn {
    min-width: 112px;
  }

  .clear-all-btn {
    min-width: 112px;
  }

  .table-container {
    flex: 1;
    overflow: hidden;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
