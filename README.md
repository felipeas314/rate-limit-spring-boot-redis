# Rate Limiting com Spring Boot, Bucket4j e Redis

API REST com rate limiting distribuido usando o algoritmo **Token Bucket** ([Bucket4j](https://bucket4j.com/)) com **Redis** como backend via [Redisson](https://redisson.org/).

O rate limit e compartilhado entre todas as instancias da aplicacao, garantindo um limite global independente de quantas replicas estejam rodando.

## Stack

| Tecnologia | Versao | Papel |
|---|---|---|
| Spring Boot | 3.2.3 | Framework web |
| Java | 17 | Linguagem |
| Bucket4j | 8.14.0 | Algoritmo token bucket (rate limiting) |
| Redisson | 3.27.1 | Cliente Redis com suporte a cluster/sentinel |
| Redis | 7 | Armazenamento distribuido dos buckets |
| Nginx | alpine | Load balancer entre instancias da app |
| Artillery | - | Teste de carga |

## Arquitetura

```
                    :8080
                   ┌──────┐
     Requests ────>│ Nginx │ (load balancer)
                   └──┬───┘
            ┌─────────┼─────────┐
            v         v         v
         app-1     app-2     app-3
            │         │         │
            └─────────┼─────────┘
                      v
              ┌─── Redis Cluster ───┐
              │ redis-1   redis-2   │
              │ redis-3   redis-4   │  6 nos (3 masters + 3 replicas)
              │ redis-5   redis-6   │
              └─────────────────────┘
```

- **Nginx** distribui as requisicoes entre as 3 instancias da app (round-robin)
- **Bucket4j** armazena o estado do bucket no **Redis**, garantindo que o rate limit e global
- **Redis Cluster** oferece alta disponibilidade e sharding

## Como funciona o rate limiting

O endpoint `GET /ratelimit` usa o algoritmo Token Bucket:

- **Capacidade:** 5 tokens
- **Reposicao:** 5 tokens a cada 1 minuto

Cada requisicao consome 1 token. Se houver tokens disponiveis, retorna **200**. Se nao, retorna **429** (Too Many Requests).

Como o bucket e armazenado no Redis (nao em memoria local), o limite de 5 req/min vale para **todas as instancias combinadas**.

## Rodando com Docker Compose

### Pre-requisitos

- Docker e Docker Compose

### Subir tudo

```bash
docker compose up --build -d
```

Isso sobe:
- 6 nos Redis formando um cluster
- 3 instancias da aplicacao
- 1 Nginx como load balancer na porta `8080`

### Derrubar tudo

```bash
docker compose down
```

## Rodando localmente (dev)

### Pre-requisitos

- Java 17+
- Redis rodando em `localhost:6379`

```bash
# Subir apenas o Redis
docker run -d -p 6379:6379 redis:7-alpine

# Rodar a aplicacao
./mvnw spring-boot:run
```

## Testando o rate limit

### Manualmente

```bash
# As primeiras 5 requisicoes retornam 200
curl http://localhost:8080/ratelimit

# A partir da 6a, retorna 429
curl http://localhost:8080/ratelimit
```

### Com Artillery (teste de carga)

```bash
npm install -g artillery
artillery run artillery.yml
```

O script envia 5 requisicoes/segundo durante 10 segundos (50 total). No relatorio, voce vera respostas **200** (primeiras 5) e **429** (restantes).

## Configuracao do Redis

A topologia do Redis e configuravel via `application.properties` ou variaveis de ambiente:

### Single (padrao para dev)

```properties
redis.mode=single
redis.address=redis://localhost:6379
```

### Sentinel (alta disponibilidade)

```properties
redis.mode=sentinel
redis.sentinel.master-name=mymaster
redis.sentinel.addresses=redis://sentinel1:26379,redis://sentinel2:26379,redis://sentinel3:26379
```

### Cluster (sharding + alta disponibilidade)

```properties
redis.mode=cluster
redis.cluster.addresses=redis://node1:6379,redis://node2:6379,redis://node3:6379
```

## Estrutura do projeto

```
src/main/java/br/com/edu/ratelimitspringbootredis/
├── RateLimitSpringBootRedisApplication.java   # Entry point
├── config/
│   ├── RedisConfig.java                       # Conexao Redis (single/sentinel/cluster)
│   └── RateLimitConfig.java                   # Configuracao do bucket (5 req/min)
└── controller/
    └── RateLimitController.java               # Endpoint GET /ratelimit

docker-compose.yml    # Redis Cluster (6 nos) + 3 apps + Nginx
Dockerfile            # Build multi-stage (Maven + JRE Alpine)
nginx.conf            # Load balancer round-robin
artillery.yml         # Script de teste de carga
```

## Endpoint

| Metodo | Path | Descricao | Respostas |
|---|---|---|---|
| GET | `/ratelimit` | Endpoint com rate limiting | `200` Success / `429` Rate limit exceeded |
