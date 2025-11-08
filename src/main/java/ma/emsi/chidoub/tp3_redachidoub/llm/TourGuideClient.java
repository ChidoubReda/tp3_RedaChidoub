package ma.emsi.chidoub.tp3_redachidoub.llm;


import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client d'accès au LLM (Gemini) pour le guide touristique.
 */
@ApplicationScoped
public class TourGuideClient {

    private static final Logger log = LoggerFactory.getLogger(TourGuideClient.class);

    // --- configuration -------------------------------------------------------
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final double TEMPERATURE = 0.4;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MEMORY_SIZE = 8;
    private static final String DEFAULT_MEAL_PRICE = "25 EUR";

    private static final List<String> DEFAULT_SPOTS = List.of(
            "Quartier historique",
            "Musée principal",
            "Parc central"
    );

    // ------------------------------------------------------------------------
    private TourGuideModel service;

    @PostConstruct
    void init() {
        String apiKey = resolveFromEnv("GEMINI_KEY", "GEMINI_API_KEY");
        if (isBlank(apiKey)) {
            log.warn("Aucune clé Gemini détectée : bascule en mode fallback.");
            return;
        }
        ChatModel chatModel = buildChatModel(apiKey);
        service = buildService(chatModel);
        log.info("Client Gemini initialisé (modèle={}, mémoire={})", MODEL_NAME, MEMORY_SIZE);
    }

    public boolean isPret() {
        return service != null;
    }

    /**
     * Retourne la réponse JSON attendue pour une destination.
     */
    public String obtenirItineraire(String destination, int nbEndroits) {
        int n = Math.max(0, nbEndroits);
        if (!isPret()) {
            return fallbackJson(destination, n);
        }
        try {
            return Objects.requireNonNull(service).genererGuide(destination, n);
        } catch (Exception ex) {
            log.warn("Appel Gemini en échec : {} — utilisation du fallback.", ex.getMessage());
            log.debug("Détails de l'erreur", ex);
            return fallbackJson(destination, n);
        }
    }

    // --- impl ---------------------------------------------------------------

    private ChatModel buildChatModel(String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(MODEL_NAME)
                .temperature(TEMPERATURE)
                .timeout(TIMEOUT)
                .build();
    }

    private TourGuideModel buildService(ChatModel chatModel) {
        return AiServices.builder(TourGuideModel.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(MEMORY_SIZE))
                .build();
    }

    private String fallbackJson(String rawDestination, int nbEndroits) {
        final String lieu = normalizeDestination(rawDestination);
        final int count = (nbEndroits > 0) ? nbEndroits : DEFAULT_SPOTS.size();

        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (int i = 0; i < count; i++) {
            String spot = DEFAULT_SPOTS.get(i % DEFAULT_SPOTS.size());
            sj.add('"' + spot + '"');
        }

        // Construction manuelle du JSON pour éviter toute dépendance supplémentaire
        return new StringBuilder(128)
                .append('{')
                .append("\"ville_ou_pays\":\"").append(escape(lieu)).append("\",")
                .append("\"endroits_a_visiter\":").append(sj.toString()).append(',')
                .append("\"prix_moyen_repas\":\"").append(DEFAULT_MEAL_PRICE).append("\",")
                .append("\"mode\":\"fallback\"")
                .append('}')
                .toString();
    }

    private String normalizeDestination(String in) {
        if (isBlank(in)) return "Destination inconnue";
        String t = in.trim();
        // Première lettre en majuscule, le reste inchangé en pratique
        return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
    }

    private String resolveFromEnv(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
