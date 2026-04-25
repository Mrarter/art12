<template>
  <div class="id-cell" :class="{ clickable: !!value }" @click="handleClick">
    <span class="id-text" :title="value">{{ displayValue }}</span>
    <el-icon v-if="value" class="copy-icon"><DocumentCopy /></el-icon>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { copyId } from '@/utils/id'

const props = defineProps({
  // ID值
  value: {
    type: String,
    default: ''
  },
  // 显示前缀标签
  label: {
    type: String,
    default: ''
  },
  // 截断显示的长度
  truncateLength: {
    type: Number,
    default: 12
  },
  // 点击复制时显示的成功消息
  successMessage: {
    type: String,
    default: '已复制'
  }
})

const displayValue = computed(() => {
  if (!props.value) return '-'
  if (props.value.length <= props.truncateLength) {
    return props.value
  }
  return props.value.substring(0, props.truncateLength) + '...'
})

const handleClick = async () => {
  if (!props.value) {
    ElMessage.warning('编号为空')
    return
  }
  copyId(
    props.value,
    () => ElMessage.success(props.successMessage),
    () => ElMessage.error('复制失败')
  )
}
</script>

<style scoped>
.id-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  color: #409eff;
  letter-spacing: 0.5px;
  
  .id-text {
    word-break: break-all;
  }
  
  .copy-icon {
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s;
    font-size: 14px;
    color: #909399;
  }
  
  &.clickable {
    cursor: pointer;
    
    &:hover .copy-icon {
      opacity: 1;
    }
  }
}
</style>
