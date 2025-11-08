package ma.emsi.chidoub.tp3_redachidoub.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Contrat de prompt pour générer un mini-guide touristique via Gemini.
 */
public interface TourGuideModel {

    @SystemMessage("""
        Rôle : tu es un guide touristique francophone.

        Exigence de sortie : réponds UNIQUEMENT en JSON valide, sans Markdown ni texte hors JSON,
        en respectant EXACTEMENT ce schéma :
        {
          "ville_ou_pays": "nom de la ville ou du pays",
          "endroits_a_visiter": ["endroit 1", "endroit 2"],
          "prix_moyen_repas": "<prix> <devise du pays>"
        }

        Règles :
        - Si aucun nombre n'est fourni ou si la valeur <= 0, liste 2 lieux (les principaux).
        - Utilise la devise officielle du pays concerné.
        - Les intitulés doivent être concis.
        """)
    @UserMessage("""
        Crée un mini-guide pour {{lieu}}.
        Donne {{n}} lieux incontournables (ou 2 si la valeur n ≤ 0) et indique le prix moyen d'un repas
        dans la devise locale.
        """)
    String genererGuide(@V("lieu") String lieu, @V("n") int n);
}
