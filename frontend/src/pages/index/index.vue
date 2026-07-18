<template>
  <!-- #ifdef H5 -->
  <OfficialHome v-if="isOfficialHost" />
  <AppHome v-else />
  <!-- #endif -->
  <!-- #ifndef H5 -->
  <AppHome />
  <!-- #endif -->
</template>

<script setup>
import AppHome from './AppHome.vue'

// #ifdef H5
import { computed } from 'vue'
import OfficialHome from './OfficialHome.vue'

const OFFICIAL_HOSTS = new Set([
  'art1.cn',
  'www.art1.cn'
])

const isNativeAppRuntime = () => {
  // #ifdef APP-PLUS
  return true
  // #endif

  if (typeof window === 'undefined') return false

  const userAgent = window.navigator?.userAgent || ''
  return /YibenArt/i.test(userAgent)
}

const isOfficialHost = computed(() => {
  if (isNativeAppRuntime()) return false
  if (typeof window === 'undefined') return false

  const { hostname, search } = window.location
  const isLocalPreview = ['127.0.0.1', 'localhost'].includes(hostname)
    && new URLSearchParams(search).get('official') === '1'

  return OFFICIAL_HOSTS.has(hostname) || isLocalPreview
})
// #endif
</script>
