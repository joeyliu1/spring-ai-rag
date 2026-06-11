<template>
  <div class="login-container">
    <el-form
      v-if="!isLoggedIn"
      :model="loginForm"
      ref="formRef"
      label-width="100px"
      v-loading="isLoading"
      element-loading-text="处理中..."
      element-loading-background="rgba(255, 255, 255, 0.8)"
      class="login-form"
    >
      <div class="form-header">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">登录到 LSS-RAG-AI 系统</p>
      </div>

      <el-form-item label="用户名" prop="userName" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]" label-width="70px">
        <el-input v-model="loginForm.userName" placeholder="请输入用户名" size="large">
          <template #prefix>
            <el-icon style="width: 20px;"><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]" label-width="70px">
        <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password>
          <template #prefix>
            <el-icon style="width: 20px;"><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label-width="0">
        <div class="btn-group">
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="isLoading">
            登录
          </el-button>
          <el-button class="register-btn" @click="showRegisterDialog">
            注册账号
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <div v-else class="profile-workbench" v-loading="isLoading">
      <section class="profile-main">
        <div class="profile-hero">
          <div class="identity-block">
            <el-avatar :size="84" :src="avatarUrl" @error="() => true" class="profile-avatar">
              {{ userInitial }}
            </el-avatar>
            <div class="identity-copy">
              <p class="eyebrow">个人中心</p>
              <h1>{{ displayName }}</h1>
              <p>{{ userInfo.userName || '未设置用户名' }} · {{ maskedPhone }}</p>
              <div class="identity-tags">
                <span class="role-badge" :class="roleClass">{{ roleLabel }}</span>
                <span class="status-badge">{{ statusLabel }}</span>
              </div>
            </div>
          </div>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="openEditProfile">
              <el-icon><Edit /></el-icon>
              编辑资料
            </el-button>
            <el-button size="large" @click="showPasswordDialog">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-button>
            <el-button size="large" @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>

        <div class="summary-grid">
          <div class="summary-card">
            <span>当前角色</span>
            <strong>{{ roleLabel }}</strong>
          </div>
          <div class="summary-card">
            <span>账号状态</span>
            <strong>{{ statusLabel }}</strong>
          </div>
          <div class="summary-card">
            <span>资料完整度</span>
            <strong>{{ profileCompletion }}%</strong>
          </div>
          <div class="summary-card">
            <span>最近更新</span>
            <strong>{{ displayUpdateTime }}</strong>
          </div>
        </div>

        <section class="profile-section">
          <div class="section-head">
            <div>
              <h2>账号资料</h2>
              <p>基础信息集中展示，修改入口放在首屏。</p>
            </div>
            <el-button text type="primary" @click="openProfileDetail">查看详情</el-button>
          </div>
          <div class="info-grid">
            <div class="info-card">
              <span>姓名</span>
              <strong>{{ userInfo.name || '-' }}</strong>
            </div>
            <div class="info-card">
              <span>用户名</span>
              <strong>{{ userInfo.userName || '-' }}</strong>
            </div>
            <div class="info-card">
              <span>手机号</span>
              <strong>{{ maskedPhone }}</strong>
            </div>
            <div class="info-card">
              <span>性别</span>
              <strong>{{ userInfo.sex || '-' }}</strong>
            </div>
            <div class="info-card">
              <span>身份证号</span>
              <strong>{{ maskedIdNumber }}</strong>
            </div>
            <div class="info-card">
              <span>创建时间</span>
              <strong>{{ displayCreateTime }}</strong>
            </div>
          </div>
        </section>
      </section>

      <aside class="profile-rail">
        <section class="rail-card">
          <div class="rail-title">
            <h3>快捷入口</h3>
            <span>常用功能</span>
          </div>
          <button class="quick-action" @click="goTo('/know-hub')">
            <span class="quick-icon blue"><el-icon><Collection /></el-icon></span>
            <span>
              <strong>我的知识库</strong>
              <small>管理文件、分块和索引</small>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button class="quick-action" @click="goTo('/ragChat')">
            <span class="quick-icon purple"><el-icon><ChatDotRound /></el-icon></span>
            <span>
              <strong>AI 问答</strong>
              <small>基于知识库开始问答</small>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button class="quick-action" @click="goTo('/draw')">
            <span class="quick-icon orange"><el-icon><PictureRounded /></el-icon></span>
            <span>
              <strong>AI 绘画</strong>
              <small>生成图片与查看结果</small>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </section>

        <section class="rail-card">
          <div class="rail-title">
            <h3>账号安全</h3>
            <span>当前状态</span>
          </div>
          <div class="status-list">
            <div class="status-item">
              <span>登录状态</span>
              <strong>已登录</strong>
            </div>
            <div class="status-item">
              <span>权限级别</span>
              <strong>{{ roleLabel }}</strong>
            </div>
            <div class="status-item">
              <span>密码</span>
              <strong>可修改</strong>
            </div>
            <div class="status-item">
              <span>资料完整度</span>
              <strong>{{ profileCompletion }}%</strong>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <!-- 个人信息对话框 -->
    <el-dialog
      v-model="profileDialogVisible"
      title="个人信息"
      width="500px"
      :close-on-click-modal="false"
      class="apple-dialog"
    >
      <div class="user-info" v-if="!isEditing">
        <div class="info-item">
          <span class="info-label"><el-icon><User /></el-icon> 姓名</span>
          <span class="info-value">{{ userInfo.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label"><el-icon><Avatar /></el-icon> 用户名</span>
          <span class="info-value">{{ userInfo.userName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label"><el-icon><Iphone /></el-icon> 手机号</span>
          <span class="info-value">{{ userInfo.phone }}</span>
        </div>
        <div class="info-item">
          <span class="info-label"><el-icon><Male /></el-icon> 性别</span>
          <span class="info-value">{{ userInfo.sex }}</span>
        </div>
        <div class="info-item">
          <span class="info-label"><el-icon><Document /></el-icon> 身份证号</span>
          <span class="info-value">{{ userInfo.idNumber }}</span>
        </div>
        <div class="info-item">
          <span class="info-label"><el-icon><Timer /></el-icon> 创建时间</span>
          <span class="info-value">{{ userInfo.createTime }}</span>
        </div>
        <div class="info-item">
          <el-button type="primary" @click="showPasswordDialog">修改密码</el-button>
        </div>
      </div>

      <el-form v-else :model="editForm" ref="editFormRef" label-width="100px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="editForm.userName" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-select v-model="editForm.sex" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男"></el-option>
            <el-option label="女" value="女"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="editForm.idNumber" placeholder="请输入身份证号"></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <template v-if="!isEditing">
            <el-button @click="profileDialogVisible = false">关闭</el-button>
            <el-button type="primary" @click="startEdit">修改</el-button>
          </template>
          <template v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="handleUpdate">保存</el-button>
          </template>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="registerDialogVisible"
      title="注册账号"
      width="500px"
      :close-on-click-modal="false"
      class="apple-dialog"
    >
      <el-form :model="registerForm" ref="registerFormRef" label-width="100px">
        <el-form-item label="用户名" prop="userName" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
          <el-input v-model="registerForm.userName" placeholder="请输入用户名">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone" :rules="[{ required: true, message: '请输入手机号', trigger: 'blur' }]">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号">
            <template #prefix>
              <el-icon><Iphone /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="姓名" prop="name" :rules="[{ required: true, message: '请输入姓名', trigger: 'blur' }]">
          <el-input v-model="registerForm.name" placeholder="请输入姓名">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-select v-model="registerForm.sex" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男"></el-option>
            <el-option label="女" value="女"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber" :rules="[{ required: true, message: '请输入身份证号', trigger: 'blur' }]">
          <el-input v-model="registerForm.idNumber" placeholder="请输入身份证号">
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="registerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRegister">注册</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="450px"
      :close-on-click-modal="false"
      class="apple-dialog"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdatePassword">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import {ElMessage, FormInstance, FormRules} from 'element-plus'
import {
  User,
  Lock,
  Avatar,
  Iphone,
  Male,
  Document,
  Timer,
  SwitchButton,
  Edit,
  Collection,
  ChatDotRound,
  PictureRounded,
  ArrowRight
} from '@element-plus/icons-vue'
import router from '@/router'
import { BASE_URL } from '@/http/config'
import { updatePasswordApi } from '@/api/UserApi'

interface UserInfo {
  id: number;
  name: string;
  userName: string;
  password: string;
  phone: string;
  sex: string;
  idNumber: string;
  status: number;
  role: string;
  createTime: string;
  updateTime: string;
  createUser: string | null;
  updateUser: string | null;
}

const loginForm = ref({
  userName: '',
  password: '',
})

const registerForm = ref({
  name: '',
  userName: '',
  password: '',
  phone: '',
  sex: '男',
  idNumber: '',
  status: 1
})

const registerDialogVisible = ref(false)
const isLoggedIn = ref(false)
const userInfo = ref<UserInfo>({
  id: 0,
  name: '',
  userName: '',
  password: '',
  phone: '',
  sex: '',
  idNumber: '',
  status: 1,
  role: 'user',
  createTime: '',
  updateTime: '',
  createUser: null,
  updateUser: null
})
const profileDialogVisible = ref(false)
const avatarUrl = ref('')
const isLoading = ref(false)
const isEditing = ref(false)
const editForm = ref({
  id: 0,
  userName: '',
  name: '',
  phone: '',
  sex: '',
  idNumber: ''
})
const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref({
  id: 0,
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayName = computed(() => userInfo.value.name || userInfo.value.userName || '当前用户')

const userInitial = computed(() => {
  const source = displayName.value || userInfo.value.userName || 'U'
  return source.trim().charAt(0).toUpperCase()
})

const roleLabel = computed(() => normalizeRole(userInfo.value.role, userInfo.value.userName) === 'admin' ? '管理员账号' : '普通用户')
const roleClass = computed(() => normalizeRole(userInfo.value.role, userInfo.value.userName) === 'admin' ? 'admin' : 'user')
const statusLabel = computed(() => userInfo.value.status === 1 ? '启用' : '停用')

const maskPhone = (phone?: string) => {
  if (!phone) return '-'
  return phone.length >= 7 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone
}

const maskIdNumber = (idNumber?: string) => {
  if (!idNumber) return '-'
  return idNumber.length >= 8 ? `${idNumber.slice(0, 4)}********${idNumber.slice(-4)}` : idNumber
}

const formatDate = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const maskedPhone = computed(() => maskPhone(userInfo.value.phone))
const maskedIdNumber = computed(() => maskIdNumber(userInfo.value.idNumber))
const displayCreateTime = computed(() => formatDate(userInfo.value.createTime))
const displayUpdateTime = computed(() => formatDate(userInfo.value.updateTime || userInfo.value.createTime))

const profileCompletion = computed(() => {
  const fields = [
    userInfo.value.name,
    userInfo.value.userName,
    userInfo.value.phone,
    userInfo.value.sex,
    userInfo.value.idNumber,
    userInfo.value.role,
    userInfo.value.createTime
  ]
  const completed = fields.filter(Boolean).length
  return Math.round((completed / fields.length) * 100)
})

const normalizeRole = (role?: string, userName?: string) => {
  if (role === 'admin' || role === 'user') {
    return role
  }
  return userName === 'admin' ? 'admin' : 'user'
}

const updateStoredRole = (role?: string, userName?: string) => {
  const normalizedRole = normalizeRole(role, userName)
  localStorage.setItem('userRole', normalizedRole)
  window.dispatchEvent(new CustomEvent('user-role-changed', {
    detail: { role: normalizedRole }
  }))
}

const fetchUserInfo = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      resetLoginState()
      return
    }
    const response = await fetch(BASE_URL + '/user/me', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    if (response.status === 401) {
      resetLoginState()
      return
    }
    const data = await response.json()
    if (data.code === 0 && data.data) {
      userInfo.value = data.data
      localStorage.setItem('userId', String(data.data.id))
      updateStoredRole(data.data.role, data.data.userName)
      isLoggedIn.value = true
    } else {
      resetLoginState()
      ElMessage({ message: data.message || '登录状态已失效，请重新登录', type: 'warning' })
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    resetLoginState()
  }
}

const resetLoginState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userRole')
  localStorage.removeItem('userId')
  window.dispatchEvent(new CustomEvent('user-role-changed', {
    detail: { role: '' }
  }))
  isLoggedIn.value = false
  profileDialogVisible.value = false
  passwordDialogVisible.value = false
  isEditing.value = false
  userInfo.value = {
    id: 0,
    name: '',
    userName: '',
    password: '',
    phone: '',
    sex: '',
    idNumber: '',
    status: 1,
    role: 'user',
    createTime: '',
    updateTime: '',
    createUser: null,
    updateUser: null
  }
}

const logout = () => {
  resetLoginState()
  router.push('/login')
  ElMessage({ message: '已成功退出登录', type: 'success' })
}

const goTo = (path: string) => {
  router.push(path)
}

const handleLogin = async () => {
  try {
    isLoading.value = true
    const response = await fetch(BASE_URL + '/user/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userName: loginForm.value.userName,
        password: loginForm.value.password,
      }),
    })
    const data = await response.json()
    if (data.code === 0) {
      localStorage.setItem('token', data.data.token)
      updateStoredRole(data.data.role, data.data.userName)
      localStorage.setItem('userId',data.data.id)

      ElMessage({ message: '登录成功', type: 'success' })
      await fetchUserInfo()
      router.push('/login')
    } else {
      ElMessage({ message: data.message, type: 'error' })
    }
  } catch (error) {
    console.error('登录错误:', error)
    ElMessage({ message: '登录失败，请稍后重试', type: 'error' })
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  try {
    isLoading.value = true
    const response = await fetch(BASE_URL+'/user/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerForm.value),
    })
    const data = await response.json()
    if (data.code === 0) {
      ElMessage({ message: '注册成功', type: 'success' })
      registerDialogVisible.value = false
      registerForm.value = {
        name: '',
        userName: '',
        password: '',
        phone: '',
        sex: '男',
        idNumber: '',
        status: 1
      }
    } else {
      ElMessage({ message: data.message, type: 'error' })
    }
  } catch (error) {
    console.error('注册错误:', error)
    ElMessage({ message: '注册失败，请稍后重试', type: 'error' })
  } finally {
    isLoading.value = false
  }
}

const showRegisterDialog = () => {
  registerDialogVisible.value = true
}

const startEdit = () => {
  editForm.value = {
    id: userInfo.value.id,
    userName: userInfo.value.userName,
    name: userInfo.value.name,
    phone: userInfo.value.phone,
    sex: userInfo.value.sex,
    idNumber: userInfo.value.idNumber
  }
  isEditing.value = true
}

const openEditProfile = () => {
  profileDialogVisible.value = true
  startEdit()
}

const openProfileDetail = () => {
  isEditing.value = false
  profileDialogVisible.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const handleUpdate = async () => {
  try {
    isLoading.value = true
    const token = localStorage.getItem('token')
    const response = await fetch(BASE_URL + '/user/update', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(editForm.value)
    })
    const data = await response.json()
    if (data.code === 0) {
      ElMessage({ message: '更新成功', type: 'success' })
      await fetchUserInfo()
      isEditing.value = false
    } else {
      ElMessage({ message: data.message || '更新失败', type: 'error' })
    }
  } catch (error) {
    console.error('更新用户信息失败:', error)
    ElMessage({ message: '更新失败，请稍后重试', type: 'error' })
  } finally {
    isLoading.value = false
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度在 6 到 64 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度在 6 到 64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度在 6 到 64 个字符', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const showPasswordDialog = () => {
  passwordForm.value = {
    id: userInfo.value.id,
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordDialogVisible.value = true
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        isLoading.value = true
        const response = await updatePasswordApi(passwordForm.value)
        if (response.code === 0) {
          ElMessage.success('密码修改成功')
          passwordDialogVisible.value = false
          passwordForm.value = {
            id: userInfo.value.id,
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          }
        } else {
          ElMessage.error(response.message || '密码修改失败')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('密码修改失败，请稍后重试')
      } finally {
        isLoading.value = false
      }
    }
  })
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    fetchUserInfo()
  }
})
</script>

<style scoped lang="less">
.login-container {
  width: 100%;
  min-height: 100%;
  height: auto;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f0f0f5 0%, #fafafa 50%, #f5f5f7 100%);
  position: relative;
  overflow: visible;
  border-radius: 18px;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at 30% 30%, rgba(0, 122, 255, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 70% 70%, rgba(175, 82, 222, 0.05) 0%, transparent 50%);
    animation: float 20s ease-in-out infinite;
    border-radius: inherit;
    pointer-events: none;
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-2%, -2%); }
}

.login-form {
  width: 360px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid var(--apple-border);
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 1;
  animation: slideUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  margin-left: -60px;

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    text-align: justify;
    padding-right: 12px;
  }

  :deep(.el-input__prefix) {
    padding-left: 8px;
  }

  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.form-header {
  text-align: center;
  margin-bottom: 24px;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.form-subtitle {
  color: var(--apple-text-secondary);
  font-size: 13px;
}

.btn-group {
  display: flex;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

.login-btn {
  min-width: 120px;
  height: 40px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%) !important;
  border: none !important;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  padding: 0 28px !important;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(0, 122, 255, 0.4) !important;
  }
}

.register-btn {
  min-width: 120px;
  height: 40px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--apple-border) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  padding: 0 28px !important;

  &:hover {
    border-color: var(--apple-blue) !important;
    color: var(--apple-blue) !important;
    background: rgba(0, 122, 255, 0.03) !important;
  }
}

.profile-workbench {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  width: 100%;
  min-height: 100%;
  padding: 0;
  animation: slideUp 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.profile-main {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 18px;
  min-width: 0;
}

.profile-hero,
.profile-section,
.rail-card,
.summary-card,
.info-card {
  border: 1px solid var(--apple-border);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
  box-shadow: var(--shadow-md);
}

.profile-hero {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  min-height: 220px;
  padding: 28px;
  overflow: hidden;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(0, 122, 255, 0.11), rgba(175, 82, 222, 0.07)),
    rgba(255, 255, 255, 0.82);

  &::after {
    content: '';
    position: absolute;
    right: -96px;
    bottom: -150px;
    width: 360px;
    height: 360px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(0, 122, 255, 0.18), transparent 64%);
    pointer-events: none;
  }
}

.identity-block {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.profile-avatar {
  flex: 0 0 auto;
  border: 4px solid rgba(255, 255, 255, 0.78);
  border-radius: 28px;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
  box-shadow: 0 18px 42px rgba(0, 122, 255, 0.28);
  color: #fff;
  font-size: 28px;
  font-weight: 800;
}

.identity-copy {
  min-width: 0;

  .eyebrow {
    margin: 0 0 8px;
    color: var(--apple-blue);
    font-size: 13px;
    font-weight: 700;
  }

  h1 {
    margin: 0;
    color: var(--apple-text-primary);
    font-size: clamp(30px, 4vw, 42px);
    line-height: 1.08;
    letter-spacing: 0;
    word-break: break-word;
  }

  p {
    margin: 10px 0 0;
    color: var(--apple-text-secondary);
    font-size: 15px;
  }
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.role-badge,
.status-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.role-badge.admin {
  color: #fff;
  background: linear-gradient(135deg, #30d158, #64d26e);
}

.role-badge.user {
  color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.1);
}

.status-badge {
  color: #248a3d;
  background: rgba(48, 209, 88, 0.14);
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;

  .el-button {
    height: 42px;
    margin-left: 0;
    border-radius: var(--radius-md);
    font-weight: 700;
  }

  .el-button--primary {
    border: none;
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
    box-shadow: 0 12px 26px rgba(0, 122, 255, 0.24);
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 108px;
  padding: 18px;
  border-radius: 18px;

  span {
    display: block;
    margin-bottom: 10px;
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  strong {
    display: block;
    color: var(--apple-text-primary);
    font-size: 24px;
    line-height: 1.2;
    word-break: break-word;
  }
}

.profile-section {
  min-height: 300px;
  padding: 20px;
  border-radius: 22px;
}

.section-head,
.rail-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;

  h2,
  h3 {
    margin: 0;
    color: var(--apple-text-primary);
    font-weight: 700;
    letter-spacing: 0;
  }

  h2 {
    font-size: 22px;
  }

  h3 {
    font-size: 17px;
  }

  p,
  span {
    margin: 6px 0 0;
    color: var(--apple-text-secondary);
    font-size: 13px;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-card {
  min-height: 86px;
  padding: 16px;
  border-radius: 16px;

  span {
    display: block;
    margin-bottom: 8px;
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  strong {
    display: block;
    color: var(--apple-text-primary);
    font-size: 16px;
    line-height: 1.45;
    word-break: break-all;
  }
}

.profile-rail {
  display: grid;
  align-content: start;
  gap: 18px;
  min-width: 0;
}

.rail-card {
  padding: 18px;
  border-radius: 22px;
}

.quick-action {
  width: 100%;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 12px;
  min-height: 66px;
  margin-top: 10px;
  padding: 12px;
  border: 1px solid var(--apple-border);
  border-radius: 16px;
  color: var(--apple-text-primary);
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(0, 122, 255, 0.22);
    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.07);
  }

  strong {
    display: block;
    font-size: 14px;
  }

  small {
    display: block;
    margin-top: 4px;
    color: var(--apple-text-secondary);
    font-size: 12px;
    line-height: 1.4;
  }

  > .el-icon {
    color: var(--apple-text-secondary);
  }
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #fff;
  font-size: 20px;

  &.blue {
    background: linear-gradient(135deg, #007aff, #5856d6);
  }

  &.purple {
    background: linear-gradient(135deg, #af52de, #5856d6);
  }

  &.orange {
    background: linear-gradient(135deg, #ff9f0a, #ffcc00);
  }
}

.status-list {
  display: grid;
  gap: 2px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--apple-border);

  &:last-child {
    border-bottom: 0;
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 13px;
  }

  strong {
    color: var(--apple-text-primary);
    font-size: 13px;
  }
}

.user-info {
  padding: 10px 0;

  .info-item {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    border-radius: var(--radius-md);
    background: rgba(0, 122, 255, 0.03);
    transition: all 0.2s ease;

    &:hover {
      background: rgba(0, 122, 255, 0.06);
    }

    .info-label {
      width: 90px;
      color: var(--apple-text-secondary);
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;

      .el-icon {
        color: var(--apple-blue);
      }
    }

    .info-value {
      color: var(--apple-text-primary);
      font-size: 14px;
      font-weight: 500;
      flex: 1;
    }
  }
}

:deep(.apple-dialog) {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;

  .el-dialog__header {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.03) 0%, rgba(175, 82, 222, 0.03) 100%);
    padding: 20px 24px !important;
    margin: 0 !important;
    border-bottom: 1px solid var(--apple-border);
  }

  .el-dialog__title {
    font-weight: 600;
    font-size: 18px;
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .el-dialog__body {
    padding: 24px !important;
  }

  .el-dialog__footer {
    padding: 16px 24px !important;
    border-top: 1px solid var(--apple-border);
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .el-button--primary {
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%) !important;
    border: none !important;
    border-radius: var(--radius-sm) !important;
    box-shadow: 0 2px 8px rgba(0, 122, 255, 0.25) !important;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 122, 255, 0.35) !important;
    }
  }
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--apple-text-primary);
}

:deep(.el-input) {
  --el-input-border-radius: var(--radius-sm);
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm) !important;
  padding: 4px 12px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: var(--radius-sm);
}

@media (max-width: 1280px) {
  .profile-workbench {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .profile-rail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .login-container {
    height: auto;
    min-height: 100vh;
    align-items: flex-start;
  }

  .profile-workbench {
    padding: 16px;
  }

  .profile-hero {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .summary-grid,
  .info-grid,
  .profile-rail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-form {
    width: min(360px, calc(100vw - 32px));
    margin-left: 0;
  }

  .profile-workbench {
    padding: 12px;
  }

  .identity-block {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-hero,
  .profile-section,
  .rail-card {
    border-radius: 18px;
    padding: 16px;
  }

  .hero-actions {
    width: 100%;

    .el-button {
      width: 100%;
    }
  }
}
</style>
