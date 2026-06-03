import http from './http'

export interface WalletTransaction {
  orderId?: number
  orderNo: string
  amount: number
  orderStatus?: number
  createTime: string
  payTime?: string
  voucherTitle?: string
  voucherSubTitle?: string
  voucherImage?: string
  statusText?: string
}

export interface WalletOverview {
  availableBalance: number
  totalSpent: number
  pendingPaymentCount: number
  availableCouponCount: number
  recentTransactions: WalletTransaction[]
}

export async function getMyWalletOverview(): Promise<WalletOverview> {
  const { data } = await http.get('/users/me/wallet')
  return data.data
}
