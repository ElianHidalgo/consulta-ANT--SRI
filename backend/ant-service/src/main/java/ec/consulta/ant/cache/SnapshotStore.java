package ec.consulta.ant.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Almacén del "último dato bueno" con su fecha de obtención. */
public interface SnapshotStore {

    <T> void put(String key, T value, Duration ttl);

    <T> Optional<Snapshot<T>> get(String key, Class<T> type);

    record Snapshot<T>(T data, Instant fetchedAt) {}
}
