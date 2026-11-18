# Modelo C4 (IcePanel)

El modelo se encuentra en IcePanel en el landscape **Consulta SRI y ANT**, con enlaces de solo lectura:

- [Nivel 1 – Contexto](https://s.icepanel.io/kSUBIBBkZIVwsj/HMm9)
- [Nivel 2 – Contenedores](https://s.icepanel.io/kSUBIBBkZIVwsj/iANb)

Cada contenedor está enlazado a su carpeta en el repositorio [consulta-ANT--SRI](https://github.com/ElianHidalgo/consulta-ANT--SRI) mediante la integración de GitHub de IcePanel.

## Nivel 1 – Contexto

| Tipo | Nombre | Descripción |
|---|---|---|
| Actor | Usuario | Persona natural que consulta su RUC, vehículo y puntos de licencia |
| Sistema | Sistema de Consulta SRI-ANT | Verifica contribuyente, vehículo y puntos de licencia |
| Sistema externo | SRI | Servicios públicos del SRI: catastro de contribuyentes y matriculación vehicular |
| Sistema externo | ANT | Web de consulta de citaciones y puntos de la Agencia Nacional de Tránsito |

| Origen → Destino | Relación | Tecnología |
|---|---|---|
| Usuario → Sistema de Consulta | Consulta RUC, matrícula y puntos | HTTPS |
| Sistema de Consulta → SRI | Verifica contribuyente y obtiene vehículo | HTTPS / REST JSON |
| Sistema de Consulta → ANT | Obtiene puntos de la licencia | HTTPS / HTML |

## Nivel 2 – Contenedores

| Tipo | Nombre | Tecnología | Responsabilidad |
|---|---|---|---|
| App | Frontend web | React 18, Vite, TypeScript, Nginx | SPA con los módulos SRI y ANT; Nginx la sirve y enruta `/api` a cada microservicio |
| App | Servicio SRI | Java 21, Spring Boot 3 | Valida RUC y placa, consulta al SRI y respalda desde la caché |
| App | Servicio ANT | Java 21, Spring Boot 3, Jsoup | Consulta la web de la ANT, extrae los puntos y respalda desde la caché |
| Store | Caché en la nube | Upstash Redis (TLS) | Guarda el último dato válido de cada consulta con su fecha y TTL |

| Origen → Destino | Relación | Tecnología |
|---|---|---|
| Usuario → Frontend web | Usa el navegador | HTTPS |
| Frontend web → Servicio SRI | Consulta contribuyente y vehículo (`/api/v1/contribuyentes`, `/api/v1/vehiculos`) | REST / JSON |
| Frontend web → Servicio ANT | Consulta puntos (`/api/v1/licencias/{cedula}/puntos`) | REST / JSON |
| Servicio SRI → SRI | Consulta catastro y matriculación | HTTPS / REST JSON |
| Servicio ANT → ANT | Consulta citaciones y puntos | HTTPS / HTML |
| Servicio SRI → Caché en la nube | Lee y guarda el último dato válido | Redis (TLS) |
| Servicio ANT → Caché en la nube | Lee y guarda el último dato válido | Redis (TLS) |

## Enlaces al código

| Objeto en IcePanel | Destino en GitHub |
|---|---|
| Sistema de Consulta SRI-ANT | Repositorio `consulta-ANT--SRI` |
| Frontend web | Carpeta `frontend` |
| Servicio SRI | Carpeta `backend/sri-service` |
| Servicio ANT | Carpeta `backend/ant-service` |

## Vista en texto

```mermaid
C4Container
  title Sistema de Consulta SRI-ANT - Contenedores
  Person(usuario, "Usuario", "Persona natural")
  System_Boundary(sis, "Sistema de Consulta SRI-ANT") {
    Container(web, "Frontend web", "React + Nginx", "Módulos SRI y ANT")
    Container(sri, "Servicio SRI", "Java 21, Spring Boot", "Contribuyente y vehículo")
    Container(ant, "Servicio ANT", "Java 21, Spring Boot", "Puntos de licencia")
    ContainerDb(cache, "Caché en la nube", "Upstash Redis", "Último dato válido")
  }
  System_Ext(sriext, "SRI", "REST JSON")
  System_Ext(antext, "ANT", "HTML")
  Rel(usuario, web, "Usa", "HTTPS")
  Rel(web, sri, "Consulta", "REST/JSON")
  Rel(web, ant, "Consulta", "REST/JSON")
  Rel(sri, sriext, "Consulta", "HTTPS")
  Rel(ant, antext, "Consulta", "HTTPS/HTML")
  Rel(sri, cache, "Lee/escribe", "Redis TLS")
  Rel(ant, cache, "Lee/escribe", "Redis TLS")
```
