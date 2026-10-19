<script setup>
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <h1>Inicio</h1>
  <p>Hola {{ auth.username }}. Tu rol es <strong>{{ auth.rol }}</strong>.</p>
</template>
