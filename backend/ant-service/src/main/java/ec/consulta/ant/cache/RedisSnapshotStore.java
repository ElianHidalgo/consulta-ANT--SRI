package ec.consulta.ant.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementación sobre Redis en la nube (Upstash). Si Redis falla, el sistema sigue funcionando
 * sin caché: nunca propaga errores de Redis al usuario.
 */
@Component
public class RedisSnapshotStore implements SnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(RedisSnapshotStore.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    /** Prefijo de las claves; en modo simulador es "sim:" para no mezclar datos simulados con los reales. */
    private final String prefijo;

    public RedisSnapshotStore(StringRedisTemplate redis, ObjectMapper mapper,
                              @Value("${app.cache.prefijo:}") String prefijo) {
        this.redis = redis;
        this.mapper = mapper;
        this.prefijo = prefijo == null ? "" : prefijo;
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        try {
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("fetchedAt", Instant.now().toString());
            envelope.set("data", mapper.valueToTree(value));
            redis.opsForValue().set(prefijo + key, mapper.writeValueAsString(envelope), ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar en caché la clave {}: {}", key, e.toString());
        }
    }

    @Override
    public <T> Optional<Snapshot<T>> get(String key, Class<T> type) {
        try {
            String raw = redis.opsForValue().get(prefijo + key);
            if (raw == null) {
                return Optional.empty();
            }
            JsonNode envelope = mapper.readTree(raw);
            T data = mapper.treeToValue(envelope.get("data"), type);
            Instant fetchedAt = Instant.parse(envelope.get("fetchedAt").asText());
            return Optional.of(new Snapshot<>(data, fetchedAt));
        } catch (Exception e) {
            log.warn("No se pudo leer la caché para la clave {}: {}", key, e.toString());
            return Optional.empty();
        }
    }
}
