import http from './http'

export interface KnowledgeBase {
  id: number
  ownerType: string
  name: string
  description?: string
  visibility: string
  documentCount: number
  totalSize: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeDocument {
  id: number
  kbId: number
  title: string
  originalFilename: string
  fileSize: number
  status: number
  parseStatus: number
  parseStatusText: 'PENDING' | 'INDEXING' | 'INDEXED' | 'FAILED' | 'UNKNOWN'
  errorMessage?: string
  chunkCount: number
  createdAt: string
  updatedAt: string
}

export async function createKnowledgeBase(payload: { name: string; description?: string }): Promise<KnowledgeBase> {
  const { data } = await http.post('/agent/knowledge-bases', payload)
  if (data.code === 200) return data.data
  throw new Error(data.message || '创建知识库失败')
}

export async function listKnowledgeBases(): Promise<KnowledgeBase[]> {
  const { data } = await http.get('/agent/knowledge-bases')
  if (data.code === 200) return data.data || []
  throw new Error(data.message || '获取知识库失败')
}

export async function getKnowledgeBase(id: number): Promise<KnowledgeBase> {
  const { data } = await http.get(`/agent/knowledge-bases/${id}`)
  if (data.code === 200) return data.data
  throw new Error(data.message || '获取知识库详情失败')
}

export async function updateKnowledgeBase(id: number, payload: { name: string; description?: string }): Promise<KnowledgeBase> {
  const { data } = await http.patch(`/agent/knowledge-bases/${id}`, payload)
  if (data.code === 200) return data.data
  throw new Error(data.message || '更新知识库失败')
}

export async function deleteKnowledgeBase(id: number): Promise<void> {
  const { data } = await http.delete(`/agent/knowledge-bases/${id}`)
  if (data.code !== 200) throw new Error(data.message || '删除知识库失败')
}

export async function listKnowledgeDocuments(kbId: number): Promise<KnowledgeDocument[]> {
  const { data } = await http.get(`/agent/knowledge-bases/${kbId}/documents`)
  if (data.code === 200) return data.data || []
  throw new Error(data.message || '获取文档列表失败')
}

export async function uploadKnowledgeDocument(kbId: number, file: File): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post(`/agent/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
  if (data.code === 200) return data.data
  throw new Error(data.message || '上传文档失败')
}

export async function deleteKnowledgeDocument(kbId: number, documentId: number): Promise<void> {
  const { data } = await http.delete(`/agent/knowledge-bases/${kbId}/documents/${documentId}`)
  if (data.code !== 200) throw new Error(data.message || '删除文档失败')
}

export async function reindexKnowledgeDocument(kbId: number, documentId: number): Promise<KnowledgeDocument> {
  const { data } = await http.post(`/agent/knowledge-bases/${kbId}/documents/${documentId}/reindex`)
  if (data.code === 200) return data.data
  throw new Error(data.message || '重建索引失败')
}
