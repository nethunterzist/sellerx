import { z } from "zod";

export const supportedMarketplaces = ["trendyol", "hepsiburada"] as const;
export type Marketplace = (typeof supportedMarketplaces)[number];

export const storeFormSchema = z.object({
  storeName: z.string().min(2, { message: "Store name is required." }),
  marketplace: z.enum(supportedMarketplaces),
  credentials: z.object({
    type: z.string(),
    apiKey: z.string().min(1, { message: "API Key is required." }),
    apiSecret: z.string().min(1, { message: "API Secret is required." }),
    sellerId: z.coerce.number().optional(),
    merchantId: z.string().optional(),
    Token: z.string().optional(),
    integrationCode: z.string().optional().or(z.literal("")),
  }),
});

export type StoreFormData = z.infer<typeof storeFormSchema>;
