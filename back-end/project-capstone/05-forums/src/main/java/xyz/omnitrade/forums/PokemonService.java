
package xyz.omnitrade.forums;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * This class implements REST endpoints to interact with Pokemon and Pokemon types.
 * The following operations are supported:
 *
 * <ul>
 * <li>GET /type: List all pokemon types</li>
 * <li>GET /pokemon: Retrieve list of all pokemons</li>
 * <li>GET /pokemon/{id}: Retrieve single pokemon by ID</li>
 * <li>GET /pokemon/name/{name}: Retrieve single pokemon by name</li>
 * <li>DELETE /pokemon/{id}: Delete a pokemon by ID</li>
 * <li>POST /pokemon: Create a new pokemon</li>
 * </ul>
 *
 * Pokémon, and Pokémon character names are trademarks of Nintendo.
 */
public class PokemonService implements Service {

    private static final Logger LOGGER = Logger.getLogger(PokemonService.class.getName());

    private final DbClient dbClient;

    PokemonService(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/type", this::listTypes)
                .get("/pokemon", this::listPokemons)
                .get("/pokemon/name/{name}", this::getPokemonByName)
                .get("/pokemon/{id}", this::getPokemonById)
                .post("/pokemon", Handler.create(Pokemon.class, this::insertPokemon))
                .delete("/pokemon/{id}", this::deletePokemonById);
    }

    private void listTypes(ServerRequest request, ServerResponse response) {
        dbClient.execute(exec -> exec.namedQuery("select-all-types"))
                .map(row -> row.as(PokemonType.class))
                .collectList()
                .forSingle(response::send);
    }

    private void listPokemons(ServerRequest request, ServerResponse response) {
        dbClient.execute(exec -> exec.namedQuery("select-all-pokemons"))
                .map(it -> it.as(Pokemon.class))
                .collectList()
                .forSingle(response::send);
    }

    private void getPokemonById(ServerRequest request, ServerResponse response) {
        try {
            int pokemonId = Integer.parseInt(request.path().param("id"));
            dbClient.execute(exec -> exec
                    .createNamedGet("select-pokemon-by-id")
                    .addParam("id", pokemonId)
                    .execute())
                    .thenAccept(maybeRow -> maybeRow
                            .ifPresentOrElse(
                                    row -> response.send(row.as(Pokemon.class)),
                                    () -> sendNotFound(response, "Pokemon " + pokemonId + " not found")))
                    .exceptionally(throwable -> sendError(throwable, response));
        } catch (NumberFormatException ex) {
            sendError(ex, response);
        }
    }

    private void getPokemonByName(ServerRequest request, ServerResponse response) {
        String pokemonName = request.path().param("name");
        dbClient.execute(exec -> exec.namedGet("select-pokemon-by-name", pokemonName))
                .thenAccept(maybeRow -> maybeRow
                        .ifPresentOrElse(
                                row -> response.send(row.as(Pokemon.class)),
                                () -> sendNotFound(response, "Pokemon " + pokemonName + " not found")))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void insertPokemon(ServerRequest request, ServerResponse response, Pokemon pokemon) {
        dbClient.execute(exec -> exec
                .createNamedInsert("insert-pokemon")
                .indexedParam(pokemon)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values\n"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void deletePokemonById(ServerRequest request, ServerResponse response) {
        try {
            int id = Integer.parseInt(request.path().param("id"));
            dbClient.execute(exec -> exec
                    .createNamedDelete("delete-pokemon-by-id")
                    .addParam("id", id)
                    .execute())
                    .thenAccept(count -> response.send("Deleted: " + count + " values\n"))
                    .exceptionally(throwable -> sendError(throwable, response));
        } catch (NumberFormatException ex) {
            sendError(ex, response);
        }
    }

    private void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }

    /**
     * Send a 500 response code and a few details.
     *
     * @param throwable throwable that caused the issue
     * @param response server response
     * @param <T> type of expected response, will be always {@code null}
     * @return {@code Void} so this method can be registered as a lambda
     * with {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}
     */
    private <T> T sendError(Throwable throwable, ServerResponse response) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }
}
