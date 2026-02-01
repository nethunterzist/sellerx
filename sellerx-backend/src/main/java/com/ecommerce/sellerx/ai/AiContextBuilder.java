package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.AiGenerateResponse;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.qa.TrendyolQuestionRepository;
import com.ecommerce.sellerx.stores.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiContextBuilder {

    private final TrendyolProductRepository productRepository;
    private final TrendyolQuestionRepository questionRepository;
    private final StoreKnowledgeBaseRepository knowledgeBaseRepository;
    private final AnswerTemplateRepository templateRepository;

    public ContextResult buildContext(TrendyolQuestion question, StoreAiSettings settings) {
        List<AiGenerateResponse.ContextSource> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        Store store = question.getStore();
        UUID storeId = store.getId();

        // 1. Ürün Bilgisi
        if (question.getBarcode() != null) {
            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, question.getBarcode());
            if (productOpt.isPresent()) {
                TrendyolProduct product = productOpt.get();
                contextBuilder.append("\n## ÜRÜN BİLGİLERİ:\n");
                contextBuilder.append("- Ürün Adı: ").append(product.getTitle()).append("\n");
                if (product.getCategoryName() != null) {
                    contextBuilder.append("- Kategori: ").append(product.getCategoryName()).append("\n");
                }
                if (product.getSalePrice() != null) {
                    contextBuilder.append("- Fiyat: ").append(product.getSalePrice()).append(" TL\n");
                }
                if (product.getBrand() != null) {
                    contextBuilder.append("- Marka: ").append(product.getBrand()).append("\n");
                }

                sources.add(AiGenerateResponse.ContextSource.builder()
                        .type("product")
                        .title(product.getTitle())
                        .snippet("Ürün bilgileri ve özellikleri")
                        .build());
            }
        }

        // 2. Geçmiş Sorular (Aynı ürün için)
        if (question.getProductId() != null) {
            List<TrendyolQuestion> historicalQuestions = questionRepository
                    .findByStoreIdAndProductIdAndStatus(
                            storeId, question.getProductId(), "ANSWERED");

            if (!historicalQuestions.isEmpty()) {
                contextBuilder.append("\n## GEÇMİŞ SORULAR VE CEVAPLAR:\n");
                int count = 0;
                for (TrendyolQuestion hq : historicalQuestions) {
                    if (count >= 3) break; // Max 3 historical Q&A
                    if (hq.getAnswers() != null && !hq.getAnswers().isEmpty()) {
                        contextBuilder.append("Soru: ").append(hq.getCustomerQuestion()).append("\n");
                        contextBuilder.append("Cevap: ").append(hq.getAnswers().get(0).getAnswerText()).append("\n\n");
                        count++;
                    }
                }

                if (count > 0) {
                    sources.add(AiGenerateResponse.ContextSource.builder()
                            .type("historical_qa")
                            .title("Geçmiş Sorular")
                            .snippet(count + " adet benzer soru ve cevap")
                            .build());
                }
            }
        }

        // 3. Bilgi Bankası
        List<StoreKnowledgeBase> knowledgeItems = knowledgeBaseRepository
                .findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId);

        if (!knowledgeItems.isEmpty()) {
            // Search for relevant knowledge based on question content
            List<StoreKnowledgeBase> relevant = findRelevantKnowledge(knowledgeItems, question.getCustomerQuestion());

            if (!relevant.isEmpty()) {
                contextBuilder.append("\n## MAĞAZA POLİTİKALARI:\n");
                for (StoreKnowledgeBase kb : relevant) {
                    contextBuilder.append("### ").append(kb.getTitle()).append(":\n");
                    contextBuilder.append(kb.getContent()).append("\n\n");

                    sources.add(AiGenerateResponse.ContextSource.builder()
                            .type("knowledge_base")
                            .title(kb.getTitle())
                            .snippet(truncate(kb.getContent(), 100))
                            .build());
                }
            }
        }

        // Build system prompt
        String systemPrompt = buildSystemPrompt(store, settings, contextBuilder.toString());

        return new ContextResult(systemPrompt, sources);
    }

    private List<StoreKnowledgeBase> findRelevantKnowledge(List<StoreKnowledgeBase> allKnowledge, String question) {
        String lowerQuestion = question.toLowerCase();
        List<StoreKnowledgeBase> relevant = new ArrayList<>();

        // Simple keyword matching
        String[] keywords = {"kargo", "iade", "garanti", "fiyat", "stok", "teslimat", "ödeme",
                "renk", "beden", "boyut", "materyal", "kutu", "şarj", "adaptör", "aksesuar"};

        for (StoreKnowledgeBase kb : allKnowledge) {
            // Check keywords array
            if (kb.getKeywords() != null) {
                for (String keyword : kb.getKeywords()) {
                    if (lowerQuestion.contains(keyword.toLowerCase())) {
                        relevant.add(kb);
                        break;
                    }
                }
            }

            // Check title and content
            if (!relevant.contains(kb)) {
                if (lowerQuestion.contains(kb.getTitle().toLowerCase()) ||
                    kb.getContent().toLowerCase().contains(lowerQuestion.substring(0, Math.min(20, lowerQuestion.length())))) {
                    relevant.add(kb);
                }
            }

            if (relevant.size() >= 5) break; // Max 5 relevant items
        }

        return relevant;
    }

    private String buildSystemPrompt(Store store, StoreAiSettings settings, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Sen bir Trendyol satıcısının müşteri hizmetleri asistanısın.\n\n");

        prompt.append("## MAĞAZA BİLGİLERİ:\n");
        prompt.append("- Mağaza Adı: ").append(store.getStoreName()).append("\n\n");

        prompt.append(context);

        prompt.append("\n## CEVAP KURALLARI:\n");

        // Tone
        String toneDescription = switch (settings.getTone()) {
            case "friendly" -> "Samimi ve sıcak bir dil kullan. Müşteriye dostça yaklaş.";
            case "formal" -> "Çok resmi bir dil kullan. Kurumsal ve ciddi ol.";
            default -> "Profesyonel ve nazik bir dil kullan.";
        };
        prompt.append("- Ton: ").append(toneDescription).append("\n");

        prompt.append("- Dil: Türkçe\n");
        prompt.append("- Maksimum uzunluk: ").append(settings.getMaxAnswerLength()).append(" karakter\n");

        if (settings.getIncludeGreeting()) {
            prompt.append("- Cevaba 'Merhaba,' ile başla.\n");
        }

        if (settings.getIncludeSignature() && settings.getSignatureText() != null) {
            prompt.append("- Cevabı '").append(settings.getSignatureText()).append("' ile bitir.\n");
        } else if (settings.getIncludeSignature()) {
            prompt.append("- Cevabı 'İyi günler dileriz, ").append(store.getStoreName()).append("' ile bitir.\n");
        }

        prompt.append("\n## ÖNEMLİ KURALLAR:\n");
        prompt.append("- Sadece bilgi bankasında ve ürün bilgilerinde yer alan bilgileri kullan.\n");
        prompt.append("- Emin olmadığın bilgileri ASLA uydurma.\n");
        prompt.append("- Bilmediğin konularda 'Bu konuda kesin bilgi veremiyorum, lütfen mağazamızla iletişime geçin.' de.\n");
        prompt.append("- Fiyat değişiklikleri veya kampanyalar için güncel bilgi vermekten kaçın.\n");

        return prompt.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    public record ContextResult(String systemPrompt, List<AiGenerateResponse.ContextSource> sources) {}
}
