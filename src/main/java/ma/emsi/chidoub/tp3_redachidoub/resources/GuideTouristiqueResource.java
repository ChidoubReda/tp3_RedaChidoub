package ma.emsi.chidoub.tp3_redachidoub.resources;


import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

import ma.emsi.chidoub.tp3_redachidoub.llm.TourGuideClient;

/**
 * Endpoints REST pour le guide touristique propulsé par un LLM.
 */
@Path("/guide")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class GuideTouristiqueResource {

    private final TourGuideClient llmClient;

    // Injection par constructeur (CDI a besoin d'un constructeur no-args aussi)
    @Inject
    public GuideTouristiqueResource(TourGuideClient llmClient) {
        this.llmClient = llmClient;
    }

    // requis par CDI
    public GuideTouristiqueResource() {
        this.llmClient = null;
    }

    @GET
    @Path("lieu/{ville_ou_pays}")
    public Response getGuide(
            @PathParam("ville_ou_pays") String lieuOuPays,
            @QueryParam("nb") @DefaultValue("0") int nbEndroits) {

        final String lieu = normalizeLieu(lieuOuPays);
        final int nb = clampNonNegative(nbEndroits);

        final String payload = requireClient().obtenirItineraire(lieu, nb);
        return Response.ok(payload).build();
    }

    // --- helpers -------------------------------------------------------------

    private TourGuideClient requireClient() {
        return Objects.requireNonNull(llmClient, "Client LLM non initialisé");
    }

    private String normalizeLieu(String raw) {
        if (raw == null) {
            throw badRequest("La destination est obligatoire");
        }
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw badRequest("La destination ne peut pas être vide");
        }
        return trimmed;
    }

    private int clampNonNegative(int n) {
        return (n < 0) ? 0 : n;
    }

    private WebApplicationException badRequest(String msg) {
        return new WebApplicationException(msg, Response.Status.BAD_REQUEST);
    }
}
