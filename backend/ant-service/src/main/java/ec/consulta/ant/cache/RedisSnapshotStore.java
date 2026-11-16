package ec.consulta.ant.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public RedisSnapshotStore(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        try {
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("fetchedAt", Instant.now().toString());
            envelope.set("data", mapper.valueToTree(value));
            redis.opsForValue().set(key, mapper.writeValueAsString(envelope), ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar en caché la clave {}: {}", key, e.toString());
        }
    }

    @Override
    public <T> Optional<Snapshot<T>> get(String key, Class<T> type) {
        try {
            String raw = redis.opsForValue().get(key);
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
