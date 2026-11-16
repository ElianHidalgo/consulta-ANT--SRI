package ec.consulta.ant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.consulta.ant.cache.SnapshotStore;
import ec.consulta.ant.domain.Resultado;
import ec.consulta.ant.domain.Resultado.Origen;
import ec.consulta.ant.exception.ApiException;
import ec.consulta.ant.exception.ServicioNoDisponibleException;
import ec.consulta.ant.exception.UpstreamException;
import ec.consulta.ant.service.ResilientFetcher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResilientFetcherTest {

    private static final Duration TTL = Duration.ofDays(7);
    private static final Duration FRESH = Duration.ofMinutes(5);

    /** Almacén en memoria que respeta el instante guardado, sin Redis. */
    static class MemoriaStore implements SnapshotStore {
        final Map<String, Snapshot<?>> datos = new HashMap<>();
        Instant ahora = Instant.parse("2026-11-15T10:00:00Z");

        @Override
        public <T> void put(String key, T value, Duration ttl) {
            datos.put(key, new Snapshot<>(value, ahora));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<Snapshot<T>> get(String key, Class<T> type) {
            return Optional.ofNullable((Snapshot<T>) datos.get(key));
        }
    }

    private MemoriaStore store;
    private Instant reloj;
    private ResilientFetcher fetcher;

    @BeforeEach
    void preparar() {
        store = new MemoriaStore();
        reloj = store.ahora;
        fetcher = new ResilientFetcher(store, new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return reloj; }
        });
    }

    @Test
    void guardaElDatoCuandoElServicioExternoResponde() {
        Resultado<String> r = fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-1");

        assertThat(r.datos()).isEqualTo("dato-1");
        assertThat(r.origen()).isEqualTo(Origen.SERVICIO_EXTERNO);
        assertThat(r.desactualizado()).isFalse();
        assertThat(store.get("k", String.class)).isPresent();
    }

    @Test
    void siFallaElServicioDevuelveElUltimoDatoBuenoMarcadoComoDesactualizado() {
        fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-bueno");
        reloj = reloj.plus(Duration.ofHours(2)); // el dato ya no es "fresco"

        Resultado<String> r = fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> {
            throw new UpstreamException("timeout");
        });

        assertThat(r.datos()).isEqualTo("dato-bueno");
        assertThat(r.origen()).isEqualTo(Origen.CACHE);
        assertThat(r.desactualizado()).isTrue();
        assertThat(r.advertencia()).contains("no respondió").contains("2026-11-15T10:00:00Z");
    }

    @Test
    void siFallaYNoHayDatoGuardadoLanzaServicioNoDisponible() {
        assertThatThrownBy(() -> fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> {
            throw new UpstreamException("caído");
        })).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void conDatoFrescoNoConsultaAlServicioExterno() {
        fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-1");
        reloj = reloj.plus(Duration.ofMinutes(1));
        AtomicInteger llamadas = new AtomicInteger();

        Resultado<String> r = fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> {
            llamadas.incrementAndGet();
            return "dato-2";
        });

        assertThat(llamadas.get()).isZero();
        assertThat(r.datos()).isEqualTo("dato-1");
        assertThat(r.origen()).isEqualTo(Origen.CACHE);
        assertThat(r.desactualizado()).isFalse();
    }

    @Test
    void conDatoViejoYServicioOkActualizaLaCache() {
        fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-1");
        reloj = reloj.plus(Duration.ofHours(1));

        Resultado<String> r = fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-2");

        assertThat(r.datos()).isEqualTo("dato-2");
        assertThat(r.origen()).isEqualTo(Origen.SERVICIO_EXTERNO);
    }

    @Test
    void losErroresDeNegocioNoActivanElRespaldo() {
        fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> "dato-1");
        reloj = reloj.plus(Duration.ofHours(1));

        assertThatThrownBy(() -> fetcher.obtener("k", String.class, TTL, FRESH, "de la ANT", () -> {
            throw ApiException.notFound("NO_CONTRIBUYENTE", "no existe");
        })).isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("codigo", "NO_CONTRIBUYENTE");
    }
}
