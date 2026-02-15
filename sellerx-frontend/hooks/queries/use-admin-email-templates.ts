import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

// Types
export interface EmailTemplate {
  id: string;
  emailType: string;
  name: string;
  subjectTemplate: string;
  bodyTemplate: string;
  description: string;
  isActive: boolean;
  availableVariables: string[];
  createdAt: string;
  updatedAt: string;
}

export interface EmailBaseLayout {
  id: string;
  headerHtml: string;
  footerHtml: string;
  styles: string;
  logoUrl: string;
  primaryColor: string;
  updatedAt: string;
}

export interface EmailTemplateUpdateRequest {
  subjectTemplate: string;
  bodyTemplate: string;
  description?: string;
  isActive?: boolean;
}

export interface EmailBaseLayoutUpdateRequest {
  headerHtml: string;
  footerHtml: string;
  styles?: string;
  logoUrl?: string;
  primaryColor: string;
}

export interface EmailPreviewRequest {
  subjectTemplate?: string;
  bodyTemplate?: string;
  sampleVariables?: Record<string, string>;
}

export interface EmailPreviewResponse {
  subject: string;
  body: string;
}

export interface EmailTestRequest {
  recipientEmail: string;
}

export interface VariableInfo {
  name: string;
  description: string;
  sampleValue: string;
}

export interface EmailVariables {
  variablesByType: Record<string, VariableInfo[]>;
}

// Query Keys
export const emailTemplateKeys = {
  all: ["admin", "email-templates"] as const,
  list: () => [...emailTemplateKeys.all, "list"] as const,
  detail: (type: string) => [...emailTemplateKeys.all, "detail", type] as const,
  variables: () => [...emailTemplateKeys.all, "variables"] as const,
  baseLayout: () => [...emailTemplateKeys.all, "base-layout"] as const,
};

// API Functions
const fetchTemplates = async (): Promise<EmailTemplate[]> => {
  const res = await fetch("/api/admin/email-templates");
  if (!res.ok) throw new Error("Failed to fetch templates");
  return res.json();
};

const fetchTemplate = async (type: string): Promise<EmailTemplate> => {
  const res = await fetch(`/api/admin/email-templates/${type}`);
  if (!res.ok) throw new Error("Failed to fetch template");
  return res.json();
};

const updateTemplate = async ({
  type,
  data,
}: {
  type: string;
  data: EmailTemplateUpdateRequest;
}): Promise<EmailTemplate> => {
  const res = await fetch(`/api/admin/email-templates/${type}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update template");
  return res.json();
};

const previewTemplate = async ({
  type,
  data,
}: {
  type: string;
  data: EmailPreviewRequest;
}): Promise<EmailPreviewResponse> => {
  const res = await fetch(`/api/admin/email-templates/${type}/preview`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to preview template");
  return res.json();
};

const sendTestEmail = async ({
  type,
  data,
}: {
  type: string;
  data: EmailTestRequest;
}): Promise<{ success: boolean; message: string }> => {
  const res = await fetch(`/api/admin/email-templates/${type}/test`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to send test email");
  return res.json();
};

const fetchVariables = async (): Promise<EmailVariables> => {
  const res = await fetch("/api/admin/email-templates/variables");
  if (!res.ok) throw new Error("Failed to fetch variables");
  return res.json();
};

const fetchBaseLayout = async (): Promise<EmailBaseLayout> => {
  const res = await fetch("/api/admin/email-templates/base-layout");
  if (!res.ok) throw new Error("Failed to fetch base layout");
  return res.json();
};

const updateBaseLayout = async (
  data: EmailBaseLayoutUpdateRequest
): Promise<EmailBaseLayout> => {
  const res = await fetch("/api/admin/email-templates/base-layout", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to update base layout");
  return res.json();
};

// Hooks

/**
 * Get all email templates
 */
export function useEmailTemplates() {
  return useQuery({
    queryKey: emailTemplateKeys.list(),
    queryFn: fetchTemplates,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Get single template by type
 */
export function useEmailTemplate(type: string) {
  return useQuery({
    queryKey: emailTemplateKeys.detail(type),
    queryFn: () => fetchTemplate(type),
    enabled: !!type,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Update template
 */
export function useUpdateEmailTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateTemplate,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: emailTemplateKeys.list() });
      queryClient.setQueryData(emailTemplateKeys.detail(variables.type), data);
    },
  });
}

/**
 * Preview template
 */
export function usePreviewEmail() {
  return useMutation({
    mutationFn: previewTemplate,
  });
}

/**
 * Send test email
 */
export function useSendTestEmail() {
  return useMutation({
    mutationFn: sendTestEmail,
  });
}

/**
 * Get all available variables
 */
export function useEmailVariables() {
  return useQuery({
    queryKey: emailTemplateKeys.variables(),
    queryFn: fetchVariables,
    staleTime: 30 * 60 * 1000, // 30 minutes - rarely changes
  });
}

/**
 * Get base layout
 */
export function useBaseLayout() {
  return useQuery({
    queryKey: emailTemplateKeys.baseLayout(),
    queryFn: fetchBaseLayout,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Update base layout
 */
export function useUpdateBaseLayout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateBaseLayout,
    onSuccess: (data) => {
      queryClient.setQueryData(emailTemplateKeys.baseLayout(), data);
    },
  });
}
