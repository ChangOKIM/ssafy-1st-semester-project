<script setup>
defineProps({
  type: {
    type: String,
    default: 'button',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  variant: {
    type: String,
    default: 'primary',
  },
})
</script>

<template>
  <button class="base-button" :class="variant" :type="type" :disabled="disabled">
    <slot />
  </button>
</template>
