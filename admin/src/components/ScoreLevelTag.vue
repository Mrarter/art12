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
  },
  certified: {
    type: Boolean,
    default: false
  }
})

const displayLevel = computed(() => {
  if (!props.level || props.level === 'U') {
    return props.certified ? '待评分' : '未评级'
  }
  return props.level
})

const tagType = computed(() => {
  if (props.level === 'S+' || props.level === 'S') return 'danger'
  if (props.level === 'A+') return 'danger'
  if (props.level === 'A') return 'warning'
  if (props.level === 'B') return 'success'
  if (!props.level || props.level === 'U') return props.certified ? 'warning' : 'info'
  return ''
})
</script>
