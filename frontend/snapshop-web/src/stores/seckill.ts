import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSeckillStore = defineStore('seckill', () => {
  const seckillToken = ref<string>('')
  const requestId = ref<string>('')
  const resultStatus = ref<string>('')
  const polling = ref(false)

  const setToken = (token: string) => {
    seckillToken.value = token
  }

  const setRequestId = (id: string) => {
    requestId.value = id
  }

  const setResultStatus = (status: string) => {
    resultStatus.value = status
  }

  const setPolling = (val: boolean) => {
    polling.value = val
  }

  const reset = () => {
    seckillToken.value = ''
    requestId.value = ''
    resultStatus.value = ''
    polling.value = false
  }

  return { seckillToken, requestId, resultStatus, polling, setToken, setRequestId, setResultStatus, setPolling, reset }
})
