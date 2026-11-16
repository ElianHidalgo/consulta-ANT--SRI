package ec.consulta.sri.service;

import ec.consulta.sri.cache.SnapshotStore;
import ec.consulta.sri.cache.SnapshotStore.Snapshot;
import ec.consulta.sri.domain.Resultado;
import ec.consulta.sri.domain.Resultado.Origen;
import ec.consulta.sri.exception.ServicioNoDisponibleException;
import ec.consulta.sri.exception.UpstreamException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Patrón "última respuesta buena" (cache-aside con respaldo obsoleto):
 * <ol>
 *   <li>Si hay un dato guardado más nuevo que {@code fresh}, se sirve de la caché.</li>
 *   <li>Si no, se consulta al servicio externo; si responde bien se guarda con TTL {@code ttl}.</li>
 *   <li>Si el servicio externo falla ({@link UpstreamException}) y hay dato guardado, se devuelve
 *       ese dato marcado como desactualizado. Sin dato guardado se responde 503.</li>
 * </ol>
 * Los errores de negocio (404, 422...) no se consideran fallas del servicio externo y se propagan.
 */
@Component
public class ResilientFetcher {

    private static final Logger log = LoggerFactory.getLogger(ResilientFetcher.class);

    private final SnapshotStore store;
    private final Clock clock;

    public ResilientFetcher(SnapshotStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public <T> Resultado<T> obtener(
            String clave, Class<T> tipo, Duration ttl, Duration fresh, String servicio, Supplier<T> externo) {

        Optional<Snapshot<T>> guardado = store.get(clave, tipo);

        if (guardado.isPresent() && esReciente(guardado.get(), fresh)) {
            return new Resultado<>(guardado.get().data(), Origen.CACHE, guardado.get().fetchedAt(), false, null);
        }

        try {
            T valor = externo.get();
            store.put(clave, valor, ttl);
            return new Resultado<>(valor, Origen.SERVICIO_EXTERNO, clock.instant(), false, null);
        } catch (UpstreamException e) {
            log.warn("{} no disponible ({}). Se intenta respaldo desde caché para {}", servicio, e.getMessage(), clave);
            if (guardado.isPresent()) {
                Snapshot<T> s = guardado.get();
                String aviso = "El servicio " + servicio + " no respondió. Se muestran los últimos datos obtenidos el "
                        + s.fetchedAt() + "; pueden no estar actualizados.";
                return new Resultado<>(s.data(), Origen.CACHE, s.fetchedAt(), true, aviso);
            }
            throw new ServicioNoDisponibleException(servicio);
        }
    }

    private boolean esReciente(Snapshot<?> s, Duration fresh) {
        if (fresh == null || fresh.isZero() || fresh.isNegative()) {
            return false;
        }
        return Duration.between(s.fetchedAt(), clock.instant()).compareTo(fresh) < 0;
    }
}
