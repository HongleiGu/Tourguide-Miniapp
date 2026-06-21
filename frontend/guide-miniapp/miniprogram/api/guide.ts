import { request } from '../shared/request'

export interface GuideMe {
  guideId: number
  name: string | null
  employmentType: string
  acceptingOrders: boolean
  status: string
  rating: number
  starLevel: number
}

export function getGuideMe(): Promise<GuideMe> {
  return request<GuideMe>({ url: '/api/guide/me' })
}

export const EMPLOYMENT_LABELS: Record<string, string> = {
  SELF: '自营',
  OUTSOURCED: '外包',
}
