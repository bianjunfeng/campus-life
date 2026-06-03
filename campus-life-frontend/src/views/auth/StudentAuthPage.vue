<template>
  <div class="student-auth-page">
    <!-- 顶部导航栏 -->
    <div class="header">
      <div class="back-button" @click="handleBack">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <h1>学生身份认证</h1>
      <div class="placeholder"></div>
    </div>

    <!-- 认证表单 -->
    <div class="auth-form">
      <!-- 提示信息 -->
      <div class="info-section">
        <div class="info-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
        </div>
        <div class="info-text">
          <p>完成学生身份认证后，您可以获得更多校园专属功能和优惠</p>
          <p class="small-text">认证信息仅用于身份验证，我们将严格保密</p>
        </div>
      </div>

      <!-- 表单字段 -->
      <div class="form-group">
        <label class="form-label">真实姓名</label>
        <input 
          type="text" 
          v-model="formData.name" 
          placeholder="请输入真实姓名" 
          class="form-input"
        >
      </div>

      <div class="form-group">
        <label class="form-label">学号</label>
        <input 
          type="text" 
          v-model="formData.studentId" 
          placeholder="请输入学号" 
          class="form-input"
        >
      </div>

      <div class="form-group">
        <label class="form-label">学校名称</label>
        <input 
          type="text" 
          v-model="formData.school" 
          placeholder="请输入学校名称" 
          class="form-input"
        >
      </div>

      <div class="form-group">
        <label class="form-label">学院/系</label>
        <input 
          type="text" 
          v-model="formData.department" 
          placeholder="请输入学院/系" 
          class="form-input"
        >
      </div>

      <div class="form-group">
        <label class="form-label">入学年份</label>
        <select v-model="formData.enrollmentYear" class="form-input">
          <option value="">请选择入学年份</option>
          <option v-for="year in availableYears" :key="year" :value="year">{{ year }}</option>
        </select>
      </div>

      <!-- 证件照片上传 -->
      <div class="form-group">
        <label class="form-label">学生证照片</label>
        <div class="upload-section" @click="triggerFileInput">
          <div class="upload-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
          </div>
          <div class="upload-text">
            <span>{{ photoFileName || '点击上传学生证照片' }}</span>
            <p class="small-text">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
          <input 
            type="file" 
            ref="fileInput" 
            accept="image/jpeg, image/png" 
            @change="handleFileUpload"
            style="display: none"
          >
        </div>
      </div>

      <!-- 提交按钮 -->
      <button class="submit-button" @click="handleSubmit" :disabled="!isFormValid || submitting || !canSubmitByStatus">
        {{ submitting ? '提交中...' : '提交认证' }}
      </button>

      <!-- 认证状态提示 -->
      <div class="status-section" v-if="authStatus">
        <div class="status-icon" :class="authStatus">
          <svg v-if="authStatus === 'approved'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <svg v-else-if="authStatus === 'pending'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <svg v-else-if="authStatus === 'rejected'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </div>
        <div class="status-text">
          <h3>
            <template v-if="authStatus === 'approved'">认证通过</template>
            <template v-else-if="authStatus === 'pending'">认证审核中</template>
            <template v-else-if="authStatus === 'rejected'">认证未通过</template>
          </h3>
          <p>
            <template v-if="authStatus === 'approved'">您已成功通过学生身份认证，可以享受校园专属功能</template>
            <template v-else-if="authStatus === 'pending'">您的认证申请正在审核中，请耐心等待</template>
            <template v-else-if="authStatus === 'rejected'">您的认证申请未通过，请检查信息后重新提交</template>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import http from '../../api/http';
import { notify } from '@/utils/notify'

const router = useRouter();

// 表单数据
const formData = ref({
  name: '',
  studentId: '',
  school: '',
  department: '',
  enrollmentYear: '',
  photo: null as File | null
});

// 文件上传相关
const fileInput = ref<HTMLInputElement | null>(null);
const photoFileName = ref('');

// 认证状态 (mock数据)
const authStatus = ref(''); // 'pending', 'approved', 'rejected', ''
const submitting = ref(false)

// 计算可用年份（最近10年）
const currentYear = new Date().getFullYear();
const availableYears = ref<number[]>([]);
for (let i = currentYear - 10; i <= currentYear; i++) {
  availableYears.value.push(i);
}

// 表单验证
const isFormValid = computed(() => {
  return formData.value.name.trim() &&
         formData.value.studentId.trim() &&
         formData.value.school.trim() &&
         formData.value.department.trim() &&
         formData.value.enrollmentYear &&
         formData.value.photo !== null;
});

const canSubmitByStatus = computed(() => authStatus.value !== 'pending' && authStatus.value !== 'approved')

// 触发文件选择
const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click();
  }
};

// 处理文件上传
const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    const file = target.files[0];
    // 检查文件大小（不超过2MB）
    if (file.size > 2 * 1024 * 1024) {
      notify('文件大小不能超过2MB');
      return;
    }
    // 检查文件类型
    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      notify('只支持JPG和PNG格式的图片');
      return;
    }
    formData.value.photo = file;
    photoFileName.value = file.name;
  }
};

// 处理返回
const handleBack = () => {
  router.back();
};

// 处理提交
const handleSubmit = async () => {
  if (submitting.value) {
    return;
  }
  if (!canSubmitByStatus.value) {
    if (authStatus.value === 'pending') {
      notify('认证申请正在审核中，暂不能再次提交');
    } else if (authStatus.value === 'approved') {
      notify('您已通过认证，无需重复提交');
    }
    return;
  }
  if (!isFormValid.value) {
    notify('请填写完整的认证信息');
    return;
  }
  
  try {
    submitting.value = true;
    // 先上传学生证照片
    let photoUrl = '';
    if (formData.value.photo) {
      const formDataUpload = new FormData();
      formDataUpload.append('file', formData.value.photo);
      
      const uploadResponse = await http.post('/upload/certificate/student', formDataUpload);
      
      if (uploadResponse.data.code === 200 && uploadResponse.data.data?.url) {
        photoUrl = uploadResponse.data.data.url;
      } else {
        notify('照片上传失败，请重试');
        return;
      }
    }
    
    // 提交认证申请
    const requestData = {
      realName: formData.value.name,
      studentNo: formData.value.studentId,
      school: formData.value.school,
      college: formData.value.department,
      grade: String(formData.value.enrollmentYear),
      studentCardImg: photoUrl
    };
    
    const response = await http.post('/auth-request/student', requestData);
    
    if (response.data.code === 200) {
      notify('认证信息提交成功，我们将尽快审核');
      authStatus.value = 'pending';
      
      // 重置表单
      formData.value = {
        name: '',
        studentId: '',
        school: '',
        department: '',
        enrollmentYear: '',
        photo: null
      };
      photoFileName.value = '';
      
      // 加载认证状态
      loadAuthStatus();
    } else {
      notify('提交失败：' + (response.data.message || '未知错误'));
    }
  } catch (error: any) {
    console.error('提交认证失败:', error);
    notify('提交失败：' + (error.response?.data?.message || error.message || '未知错误'));
  } finally {
    submitting.value = false;
  }
};

// 加载认证状态
const loadAuthStatus = async () => {
  try {
    const response = await http.get('/auth-request/student');
    if (response.data.code === 200 && response.data.data?.hasRequest) {
      const status = response.data.data.status;
      if (status === 0) {
        authStatus.value = 'pending';
      } else if (status === 1) {
        authStatus.value = 'approved';
      } else if (status === 2) {
        authStatus.value = 'rejected';
      }
    }
  } catch (error) {
    console.error('加载认证状态失败:', error);
  }
};

// 页面加载时获取认证状态
onMounted(() => {
  loadAuthStatus();
});
</script>

<style scoped>
.student-auth-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

/* 顶部导航栏 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  z-index: 100;
}

.back-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.back-button:hover {
  background-color: #f5f5f5;
}

.back-button svg {
  width: 24px;
  height: 24px;
  color: #333;
}

.header h1 {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.placeholder {
  width: 40px;
}

/* 表单容器 */
.auth-form {
  margin-top: 50px;
  padding: 20px;
  background-color: white;
  min-height: calc(100vh - 50px);
}

/* 信息提示部分 */
.info-section {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background-color: #f0f9ff;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-icon {
  width: 28px;
  height: 28px;
  color: #1677ff;
  margin-right: 12px;
  margin-top: 2px;
}

.info-text p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.info-text .small-text {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}

/* 表单组 */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
}

/* 上传区域 */
.upload-section {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #fafafa;
}

.upload-section:hover {
  border-color: #1677ff;
  background-color: #f0f9ff;
}

.upload-icon {
  width: 40px;
  height: 40px;
  color: #999;
  margin-right: 16px;
}

.upload-text span {
  display: block;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.upload-text .small-text {
  font-size: 12px;
  color: #999;
  margin: 0;
}

/* 提交按钮 */
.submit-button {
  width: 100%;
  padding: 14px;
  background-color: #1677ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-button:hover {
  background-color: #0958d9;
}

.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 状态提示 */
.status-section {
  margin-top: 30px;
  padding: 20px;
  border-radius: 8px;
  background-color: #fafafa;
  display: flex;
  align-items: center;
}

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.status-icon.approved {
  background-color: #f6ffed;
  color: #52c41a;
}

.status-icon.pending {
  background-color: #fff7e6;
  color: #fa8c16;
}

.status-icon.rejected {
  background-color: #fff2f0;
  color: #ff4d4f;
}

.status-icon svg {
  width: 28px;
  height: 28px;
}

.status-text h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
}

.status-text p {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .auth-form {
    padding: 16px;
  }
  
  .form-input {
    font-size: 14px;
    padding: 10px;
  }
}
</style>
