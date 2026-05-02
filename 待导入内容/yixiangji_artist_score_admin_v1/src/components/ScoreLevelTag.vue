<template>
  <el-tag :type="tagType" effect="dark">
    {{ level || 'D' }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: {
    type: String,
    default: 'D'
  }
})

const tagType = computed(() => {
  if (props.level === 'A+') return 'danger'
  if (props.level === 'A') return 'warning'
  if (props.level === 'B') return 'success'
  if (props.level === 'C') return 'info'
  return ''
})
</script>
