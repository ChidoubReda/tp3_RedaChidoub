package ma.emsi.chidoub.tp3_redachidoub.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.emsi.chidoub.tp3_redachidoub.llm.TourGuideClient;

/**
 * Variante de la ressource REST pour le bonus :
 * - accepte /lieu/{ville_ou_pays}/{nb}
 * - OU /lieu/{ville_ou_pays}?nb=...
 */
@Path("/guideplus")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class GuideTouristiqueResourceBonus {

    @Inject
    private TourGuideClient client;

    // --- Variante 1 : paramètre de requête
    // Exemple : /api/guideplus/lieu/Paris?nb=3
    @GET
    @Path("lieu/{ville_ou_pays}")
    public Response getGuideQuery(
            @PathParam("ville_ou_pays") String lieu,
            @QueryParam("nb") @DefaultValue("0") int nb) {
        return buildResponse(lieu, nb);
    }

    // --- Variante 2 : segment de chemin
    // Exemple : /api/guideplus/lieu/Paris/3
    @GET
    @Path("lieu/{ville_ou_pays}/{nb}")
    public Response getGuidePath(
            @PathParam("ville_ou_pays") String lieu,
            @PathParam("nb") int nb) {
        return buildResponse(lieu, nb);
    }

    // Méthode interne commune
    private Response buildResponse(String lieu, int nb) {
        if (lieu == null || lieu.isBlank()) {
            throw new WebApplicationException("La destination est obligatoire", Response.Status.BAD_REQUEST);
        }
        int count = Math.max(0, nb);
        String json = client.obtenirItineraire(lieu.trim(), count);

        return Response.ok(json)
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }
}
