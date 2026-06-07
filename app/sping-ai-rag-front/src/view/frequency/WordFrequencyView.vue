<template>
  <div class="word-frequency">
    <el-card class="box-card">
      <div class="chart-header">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="true"
          @change="loadWordFrequencyData"
        />
        <el-select v-model="businessType" placeholder="问答场景" clearable @change="loadWordFrequencyData">
          <el-option label="全部场景" value="" />
          <el-option label="普通聊天" value="chat" />
          <el-option label="RAG 问答" value="rag_chat" />
        </el-select>
        <el-button type="primary" @click="loadWordFrequencyData">刷新数据</el-button>
        <el-button type="danger" @click="handleClean">清空数据</el-button>
      </div>
      
      <div class="charts-container">
        <!-- 词云图 -->
        <div class="chart-item">
          <h3>词云展示</h3>
          <div 
            ref="wordCloudRef"
            v-loading="isLoading"
            element-loading-text="加载中..."
            element-loading-background="rgba(255, 255, 255, 0.8)"
            class="chart-box"
          ></div>
        </div>

        <!-- TOP10词频柱状图 -->
        <div class="chart-item">
          <h3>TOP10热词统计</h3>
          <div 
            ref="barChartRef"
            v-loading="isLoading"
            class="chart-box"
          ></div>
        </div>

        <!-- 词频分布饼图 -->
        <div class="chart-item">
          <h3>词频分布</h3>
          <div 
            ref="pieChartRef"
            v-loading="isLoading"
            class="chart-box"
          ></div>
        </div>

        <!-- 词频趋势图 -->
        <div class="chart-item">
          <h3>热词趋势</h3>
          <div 
            ref="lineChartRef"
            v-loading="isLoading"
            class="chart-box"
          ></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { cleanFrequencyApi, listFrequencyApi } from '@/api/FrequencyApi'

// 图表引用
const wordCloudRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)

// 图表实例
const charts = ref<{[key: string]: echarts.ECharts | null}>({
  wordCloud: null,
  barChart: null,
  pieChart: null,
  lineChart: null
})

const isLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const businessType = ref('')

// 初始化所有图表
const initCharts = () => {
  if (wordCloudRef.value) {
    charts.value.wordCloud = echarts.init(wordCloudRef.value)
  }
  if (barChartRef.value) {
    charts.value.barChart = echarts.init(barChartRef.value)
  }
  if (pieChartRef.value) {
    charts.value.pieChart = echarts.init(pieChartRef.value)
  }
  if (lineChartRef.value) {
    charts.value.lineChart = echarts.init(lineChartRef.value)
  }
}

// 加载词频数据
const loadWordFrequencyData = async () => {
  if (!charts.value.wordCloud) return
  isLoading.value = true
  
  try {
    const response = await listFrequencyApi({
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      businessType: businessType.value || undefined
    })
    
    if (response.code === 0 && response.data) {
      const rawData = response.data.map((item: any) => ({
        name: item.word,
        value: item.countNum,
        businessType: item.businessType,
        date: formatDate(item.createTime)
      }))
      const data = aggregateByWord(rawData)

      // 设置词云图
      const wordCloudOption = {
        tooltip: {
          show: true
        },
        series: [{
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '90%',
          height: '90%',
          right: null,
          bottom: null,
          sizeRange: [12, 60],
          rotationRange: [-90, 90],
          rotationStep: 45,
          gridSize: 8,
          drawOutOfBound: false,
          textStyle: {
            fontFamily: 'sans-serif',
            fontWeight: 'bold',
            color: function () {
              return 'rgb(' + [
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160)
              ].join(',') + ')'
            }
          },
          emphasis: {
            focus: 'self',
            textStyle: {
              shadowBlur: 10,
              shadowColor: '#333'
            }
          },
          data: data
        }]
      }

      // 设置TOP10柱状图
      const top10Data = [...data]
        .sort((a, b) => b.value - a.value)
        .slice(0, 10)
      
      const barOption = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: top10Data.map(item => item.name),
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '词频',
          type: 'bar',
          data: top10Data.map(item => item.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' }
            ])
          }
        }]
      }

      // 设置词频分布饼图
      const frequencyRanges = [
        { name: '极高频(>10000)', min: 10000, max: Infinity },
        { name: '高频(1000-10000)', min: 1000, max: 10000 },
        { name: '中频(100-1000)', min: 100, max: 1000 },
        { name: '低频(10-100)', min: 10, max: 100 },
        { name: '极低频(<10)', min: 0, max: 10 }
      ]

      const pieData = frequencyRanges.map(range => ({
        name: range.name,
        value: data.filter(item => item.value >= range.min && item.value < range.max).length
      }))

      const pieOption = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [{
          name: '词频分布',
          type: 'pie',
          radius: '50%',
          data: pieData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      }

      // 设置趋势图（取TOP5词的真实每日趋势）
      const top5Words = [...data]
        .sort((a, b) => b.value - a.value)
        .slice(0, 5)
        .map(item => item.name)
      const trendDates = buildTrendDates(rawData)

      const lineOption = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: top5Words
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: trendDates
        },
        yAxis: {
          type: 'value'
        },
        series: top5Words.map(word => ({
          name: word,
          type: 'line',
          data: trendDates.map(date => sumWordByDate(rawData, word, date))
        }))
      }

      // 更新所有图表
      charts.value.wordCloud?.setOption(wordCloudOption)
      charts.value.barChart?.setOption(barOption)
      charts.value.pieChart?.setOption(pieOption)
      charts.value.lineChart?.setOption(lineOption)
    } else {
      ElMessage.error(response.message || '获取数据失败')
    }
  } catch (error) {
    console.error('Error:', error)
    ElMessage.error('获取数据失败')
  } finally {
    isLoading.value = false
  }
}

const aggregateByWord = (rawData: Array<{ name: string; value: number; date: string }>) => {
  const wordMap = new Map<string, number>()
  rawData.forEach(item => {
    wordMap.set(item.name, (wordMap.get(item.name) || 0) + item.value)
  })
  return Array.from(wordMap.entries()).map(([name, value]) => ({ name, value }))
}

const buildTrendDates = (rawData: Array<{ date: string }>) => {
  const dates = Array.from(new Set(rawData.map(item => item.date).filter(Boolean))).sort()
  if (dates.length > 0) {
    return dates.slice(-7)
  }
  return getLastSevenDays()
}

const sumWordByDate = (
  rawData: Array<{ name: string; value: number; date: string }>,
  word: string,
  date: string
) => {
  return rawData
    .filter(item => item.name === word && item.date === date)
    .reduce((sum, item) => sum + item.value, 0)
}

const formatDate = (value: string) => {
  if (!value) return ''
  return value.slice(0, 10)
}

const getLastSevenDays = () => {
  const dates: string[] = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    dates.push(date.toISOString().slice(0, 10))
  }
  return dates
}

// 清空数据
const handleClean = () => {
  ElMessageBox.confirm(
    '确定要清空所有词频数据吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const response = await cleanFrequencyApi()
      if (response.code === 0) {
        ElMessage.success('清空数据成功')
        loadWordFrequencyData()
      } else {
        ElMessage.error(response.message || '清空数据失败')
      }
    } catch (error) {
      console.error('Error:', error)
      ElMessage.error('清空数据失败')
    }
  }).catch(() => {
    ElMessage.info('已取消清空操作')
  })
}

// 监听窗口大小变化
const handleResize = () => {
  Object.values(charts.value).forEach(chart => chart?.resize())
}

onMounted(() => {
  initCharts()
  loadWordFrequencyData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(charts.value).forEach(chart => chart?.dispose())
})
</script>

<style scoped lang="less">
.word-frequency {
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

  .chart-header {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    
    .el-button {
      margin-right: 0;
    }

    .el-select {
      width: 140px;
    }
  }

  .charts-container {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
    gap: 20px;
    overflow: hidden;
    
    .chart-item {
      background-color: #fff;
      border-radius: 4px;
      padding: 10px;
      box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
      
      h3 {
        margin: 0 0 10px 0;
        font-size: 16px;
        color: #333;
      }
      
      .chart-box {
        width: 100%;
        height: calc(100% - 30px);
      }
    }
  }
}
</style>
