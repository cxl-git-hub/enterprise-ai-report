<template>
  <div class="basic-layout fullscreen-center">
    <slot />
  </div>
</template>

<style lang="scss" scoped>
.basic-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
