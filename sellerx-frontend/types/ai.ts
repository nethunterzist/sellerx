// AI Settings
export interface AiSettings {
  id: string;
  storeId: string;
  aiEnabled: boolean;
  autoAnswer: boolean;
  tone: "professional" | "friendly" | "formal";
  language: string;
  maxAnswerLength: number;
  includeGreeting: boolean;
  includeSignature: boolean;
  signatureText?: string;
  confidenceThreshold: number;
}

// Knowledge Base
export interface KnowledgeBaseItem {
  id: string;
  storeId: string;
  category: string;
  title: string;
  content: string;
  keywords: string[];
  isActive: boolean;
  priority: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateKnowledgeRequest {
  category: string;
  title: string;
  content: string;
  keywords: string[];
  priority?: number;
}

// AI Answer Generation
export interface ContextSource {
  type: "product" | "historical_qa" | "knowledge_base" | "template";
  title: string;
  snippet?: string;
}

export interface AiGenerateResponse {
  generatedAnswer: string;
  confidenceScore: number;
  contextSources: ContextSource[];
  modelVersion: string;
  tokensUsed: number;
  generationTimeMs: number;
  logId: string;
  crossSellProducts?: { barcode: string; title: string; image?: string; salePrice: number }[];
}

export interface AiApproveRequest {
  logId: string;
  finalAnswer: string;
}

// Knowledge Base Categories
export const KNOWLEDGE_CATEGORIES = [
  { value: "shipping", label: "Kargo" },
  { value: "returns", label: "İade" },
  { value: "payment", label: "Ödeme" },
  { value: "product", label: "Ürün" },
  { value: "general", label: "Genel" },
] as const;

export type KnowledgeCategory = (typeof KNOWLEDGE_CATEGORIES)[number]["value"];
