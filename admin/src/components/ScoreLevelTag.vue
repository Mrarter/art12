<template>
  <el-tag :type="tagType" effect="dark">
    {{ displayLevel }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: {
    type: String,
    default: 'U'
  }
})

const displayLevel = computed(() => {
  if (props.level === 'U') return '未认证'
  return props.level || '未认证'
})

const tagType = computed(() => {
  if (props.level === 'S+' || props.level === 'S') return 'danger'
  if (props.level === 'A+') return 'danger'
  if (props.level === 'A') return 'warning'
  if (props.level === 'B') return 'success'
  if (props.level === 'U') return 'info'
  return ''
})
</script>
