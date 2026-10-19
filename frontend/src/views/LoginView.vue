<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const enviando = ref(false)

async function ingresar() {
  error.value = ''
  enviando.value = true
  try {
    await auth.login(username.value, password.value)
    // Si lo mandaron al login desde otra pagina, vuelve a esa
    router.push(route.query.redirect || '/')
  } catch (e) {
    error.value = e.message
  } finally {
    enviando.value = false
  }
}
</script>

<template>
  <form class="login" @submit.prevent="ingresar">
    <h1>Ingresar</h1>
    <label>
      Usuario
      <input v-model="username" autocomplete="username" required />
    </label>
    <label>
      Contraseña
      <input v-model="password" type="password" autocomplete="current-password" required />
    </label>
    <p v-if="error" class="error">{{ error }}</p>
    <button type="submit" :disabled="enviando">Entrar</button>
  </form>
</template>
