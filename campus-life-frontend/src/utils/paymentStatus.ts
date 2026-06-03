export type PaymentDisplayState = 'success' | 'pending' | 'failed' | 'refunded'

const SUCCESS_PAYMENT_STATUSES = new Set([
  'SUCCESS',
  'TRADE_SUCCESS',
  'TRADE_FINISHED'
])

const REFUNDED_PAYMENT_STATUSES = new Set([
  'REFUNDED',
  'FULL_REFUNDED',
  'PARTIAL_REFUNDED'
])

const FAILED_PAYMENT_STATUSES = new Set([
  'FAILED',
  'TRADE_CLOSED',
  'CLOSED',
  'CANCELLED'
])

export const resolvePaymentDisplayState = (
  paymentStatus?: string,
  orderStatus?: number | string | null
): PaymentDisplayState => {
  const normalizedPaymentStatus = String(paymentStatus || '').toUpperCase()
  const normalizedOrderStatus = Number(orderStatus)

  if (REFUNDED_PAYMENT_STATUSES.has(normalizedPaymentStatus) || normalizedOrderStatus === 4) {
    return 'refunded'
  }

  if (SUCCESS_PAYMENT_STATUSES.has(normalizedPaymentStatus) || normalizedOrderStatus === 1 || normalizedOrderStatus === 2) {
    return 'success'
  }

  if (FAILED_PAYMENT_STATUSES.has(normalizedPaymentStatus) || normalizedOrderStatus === 3 || normalizedOrderStatus === 5) {
    return 'failed'
  }

  return 'pending'
}
