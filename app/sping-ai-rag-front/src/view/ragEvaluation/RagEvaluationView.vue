<template>
  <div class="rag-evaluation-page">
    <div class="page-header">
      <div>
        <h2>RAG 评测</h2>
        <p>批量对比 topK 和相似度阈值策略，仅管理员可见。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadSampleCases">载入样例</el-button>
        <el-button @click="addCase">新增用例</el-button>
        <el-button type="primary" :loading="isRunning" @click="runEvaluation">开始测评</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-title">策略配置</div>
      <div class="strategy-grid">
        <div class="strategy-item">
          <span>topK 列表</span>
          <el-input v-model="topKText" placeholder="3,5,10" />
        </div>
        <div class="strategy-item">
          <span>相似度阈值列表</span>
          <el-input v-model="thresholdText" placeholder="0.1,0.2" />
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">评测用例</div>
      <el-table :data="cases" border class="case-table">
        <el-table-column label="问题" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row.question" type="textarea" :rows="2" placeholder="输入问题" />
          </template>
        </el-table-column>
        <el-table-column label="来源文件" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.expectedSourcesText" placeholder="guide.pdf,faq.docx" />
          </template>
        </el-table-column>
        <el-table-column label="关键词" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.expectedKeywordsText" placeholder="RAG,Milvus,来源" />
          </template>
        </el-table-column>
        <el-table-column label="检索文件过滤" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.sourcesText" placeholder="guide.pdf" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeCase($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel" v-if="batchResult">
      <div class="panel-title">策略对比结果</div>
      <div class="summary-grid">
        <div class="summary-item">
          <span>用例数</span>
          <strong>{{ batchResult.caseCount }}</strong>
        </div>
        <div class="summary-item">
          <span>策略数</span>
          <strong>{{ batchResult.strategyCount }}</strong>
        </div>
      </div>

      <el-table :data="batchResult.strategies" border class="result-table">
        <el-table-column prop="topK" label="topK" width="90" />
        <el-table-column prop="similarityThreshold" label="阈值" width="110" />
        <el-table-column prop="passRate" label="通过率" width="110">
          <template #default="{ row }">{{ formatRate(row.passRate) }}</template>
        </el-table-column>
        <el-table-column prop="recallAtK" label="Recall@K" width="110">
          <template #default="{ row }">{{ formatRate(row.recallAtK) }}</template>
        </el-table-column>
        <el-table-column prop="meanReciprocalRank" label="MRR" width="110">
          <template #default="{ row }">{{ formatRate(row.meanReciprocalRank) }}</template>
        </el-table-column>
        <el-table-column prop="averageRetrievedCount" label="平均召回数" width="120" />
        <el-table-column prop="averageScore" label="平均相似度" width="120" />
        <el-table-column prop="averageSourceHitRate" label="来源命中率" width="120">
          <template #default="{ row }">{{ formatRate(row.averageSourceHitRate) }}</template>
        </el-table-column>
        <el-table-column prop="averageKeywordHitRate" label="关键词命中率" width="120">
          <template #default="{ row }">{{ formatRate(row.averageKeywordHitRate) }}</template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <el-table :data="row.results" size="small" border>
                <el-table-column prop="question" label="问题" min-width="220" />
                <el-table-column prop="passed" label="通过" width="90">
                  <template #default="{ row: caseRow }">{{ caseRow.passed ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column prop="retrievedCount" label="召回数" width="90" />
                <el-table-column prop="averageScore" label="平均相似度" width="110" />
                <el-table-column prop="sourceHitRate" label="来源命中率" width="110">
                  <template #default="{ row: caseRow }">{{ formatRate(caseRow.sourceHitRate) }}</template>
                </el-table-column>
                <el-table-column prop="keywordHitRate" label="关键词命中率" width="120">
                  <template #default="{ row: caseRow }">{{ formatRate(caseRow.keywordHitRate) }}</template>
                </el-table-column>
                <el-table-column prop="retrievedChunks" label="召回片段" min-width="160">
                  <template #default="{ row: caseRow }">
                    <span>{{ caseRow.retrievedChunks?.length || 0 }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { evaluateRagBatchApi } from "@/api/RagEvaluationApi";

interface EvalCaseRow {
  question: string;
  sourcesText: string;
  expectedSourcesText: string;
  expectedKeywordsText: string;
}

const isRunning = ref(false);
const topKText = ref("3,5,10");
const thresholdText = ref("0.1,0.2");
const cases = ref<EvalCaseRow[]>([]);
const batchResult = ref<any>(null);

const sampleCases: EvalCaseRow[] = [
  { question: "系统支持哪些知识库文件管理能力？", sourcesText: "", expectedSourcesText: "", expectedKeywordsText: "上传,删除,重建索引,分块预览" },
  { question: "RAG 问答如何展示回答来源？", sourcesText: "", expectedSourcesText: "", expectedKeywordsText: "来源,相似度,分块" },
  { question: "递归分块策略包含哪些可配置参数？", sourcesText: "", expectedSourcesText: "", expectedKeywordsText: "chunkSize,overlap,minChunkSize,maxChunks" },
  { question: "敏感词审核支持哪些链路？", sourcesText: "", expectedSourcesText: "", expectedKeywordsText: "输入,输出,拦截,日志" },
  { question: "热点词分析的数据来自哪里？", sourcesText: "", expectedSourcesText: "", expectedKeywordsText: "用户提问,分词,统计" },
];

const loadSampleCases = () => {
  cases.value = sampleCases.map(item => ({ ...item }));
  if (!batchResult.value) {
    ElMessage.success("已载入样例");
  }
};

const addCase = () => {
  cases.value.push({
    question: "",
    sourcesText: "",
    expectedSourcesText: "",
    expectedKeywordsText: "",
  });
};

const removeCase = (index: number) => {
  cases.value.splice(index, 1);
};

const parseNumberList = (text: string) => {
  return text
    .split(",")
    .map(item => Number(item.trim()))
    .filter(item => Number.isFinite(item));
};

const parseStringList = (text: string) => {
  return text
    .split(",")
    .map(item => item.trim())
    .filter(Boolean);
};

const runEvaluation = async () => {
  const payload = {
    cases: cases.value
      .filter(item => item.question.trim())
      .map(item => ({
        question: item.question.trim(),
        sources: parseStringList(item.sourcesText),
        expectedSources: parseStringList(item.expectedSourcesText),
        expectedKeywords: parseStringList(item.expectedKeywordsText),
      })),
    topKValues: parseNumberList(topKText.value),
    similarityThresholds: parseNumberList(thresholdText.value),
  };

  if (payload.cases.length === 0) {
    ElMessage.warning("请至少填写一个有效用例");
    return;
  }
  if (payload.topKValues.length === 0 || payload.similarityThresholds.length === 0) {
    ElMessage.warning("请填写有效的 topK 和阈值列表");
    return;
  }

  isRunning.value = true;
  try {
    const res = await evaluateRagBatchApi(payload);
    if (res.code === 0) {
      batchResult.value = res.data;
      ElMessage.success("测评完成");
    } else {
      ElMessage.error(res.message || "测评失败");
    }
  } catch (error) {
    console.error(error);
    ElMessage.error("测评失败");
  } finally {
    isRunning.value = false;
  }
};

const formatRate = (value: number) => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return `${(value * 100).toFixed(1)}%`;
};

onMounted(() => {
  loadSampleCases();
});
</script>

<style scoped lang="less">
.rag-evaluation-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.panel {
  padding: 16px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-lg);
  background: var(--apple-card);
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-sm);
}

.panel-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.strategy-grid,
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.strategy-item,
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;

  span {
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  strong {
    color: var(--apple-text-primary);
    font-size: 18px;
  }
}

.case-table,
.result-table {
  width: 100%;
}

.expand-box {
  padding: 8px 0;
}
</style>
