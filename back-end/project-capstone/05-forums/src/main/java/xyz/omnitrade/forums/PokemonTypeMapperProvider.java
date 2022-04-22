
package xyz.omnitrade.forums;

import javax.annotation.Priority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.spi.DbMapperProvider;

/**
 * Provider for PokemonType mappers.
 *
 * Pokémon, and Pokémon character names are trademarks of Nintendo.
 */
@Priority(1000)
public class PokemonTypeMapperProvider implements DbMapperProvider {
    private static final PokemonTypeMapper MAPPER = new PokemonTypeMapper();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        return type.equals(PokemonType.class) ? Optional.of((DbMapper<T>) MAPPER) : Optional.empty();
    }

    /**
     * Maps database types to a PokemonType class.
     */
    static class PokemonTypeMapper implements DbMapper<PokemonType> {

        @Override
        public PokemonType read(DbRow row) {
            DbColumn id = row.column("ID");
            DbColumn name = row.column("NAME");
            return new PokemonType(id.as(Integer.class), name.as(String.class));
        }

        @Override
        public Map<String, ?> toNamedParameters(PokemonType value) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public List<?> toIndexedParameters(PokemonType value) {
            throw new UnsupportedOperationException("Not supported");
        }
    }
}
