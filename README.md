# Spring Boot Redis Cache


Context:

  - [**Comecando**](#comecando)
  - [**Maven Dependencies**](#maven-dependencies)
  - [**Redis Configuration**](#redis-configuration)
  - [**Spring Service**](#spring-service)
  - [**Docker & Docker Compose**](#docker-docker-compose)
  - [**Fazendo Build & Rodando a Aplicação**](#fazendo-build-rodando-a-aplicação)
  - [**Endpoints com Swagger**](#endpoints-com-swagger)


## Comecando

Neste projeto, foi usado Redis para cache com Spring Boot.
Ao enviar qualquer solicitação para obter todos os clientes ou cliente por id, você aguardará 3 segundos se o Redis não tiver dados relacionados.


## Maven Dependencies


```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
	<groupId>redis.clients</groupId>
	<artifactId>jedis</artifactId>
</dependency>
		
```

## Redis Configuration

```java
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
public class RedisConfig {

	@Autowired
	private CacheManager cacheManager;

	@Value("${spring.redis.host}")
	private String redisHost;

	@Value("${spring.redis.port}")
	private int redisPort;

	@Bean
	public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Serializable> template = new RedisTemplate<>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory factory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
		RedisCacheConfiguration redisCacheConfiguration = config
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new GenericJackson2JsonRedisSerializer()));
		RedisCacheManager redisCacheManager = RedisCacheManager.builder(factory).cacheDefaults(redisCacheConfiguration)
				.build();
		return redisCacheManager;
	}

}
```


## Spring Service

A implementação do atendimento ao cliente do Spring Boot será como a classe abaixo.
Foi usado Spring Boot Cache @Annotaions para armazenamento em cache.

São eles:

* `@Cacheable`
* `@CacheEvict`
* `@Caching`
* `@CachceConfig`
	

```java
@Service
@CacheConfig(cacheNames = "customerCache")
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	private CustomerRepository customerRepository;

	@Cacheable(cacheNames = "customers")
	@Override
	public List<Customer> getAll() {
		waitSomeTime();
		return this.customerRepository.findAll();
	}

	@CacheEvict(cacheNames = "customers", allEntries = true)
	@Override
	public Customer add(Customer customer) {
		return this.customerRepository.save(customer);
	}

	@CacheEvict(cacheNames = "customers", allEntries = true)
	@Override
	public Customer update(Customer customer) {
		Optional<Customer> optCustomer = this.customerRepository.findById(customer.getId());
		if (!optCustomer.isPresent())
			return null;
		Customer repCustomer = optCustomer.get();
		repCustomer.setName(customer.getName());
		repCustomer.setContactName(customer.getContactName());
		repCustomer.setAddress(customer.getAddress());
		repCustomer.setCity(customer.getCity());
		repCustomer.setPostalCode(customer.getPostalCode());
		repCustomer.setCountry(customer.getCountry());
		return this.customerRepository.save(repCustomer);
	}

	@Caching(evict = { @CacheEvict(cacheNames = "customer", key = "#id"),
			@CacheEvict(cacheNames = "customers", allEntries = true) })
	@Override
	public void delete(long id) {
		this.customerRepository.deleteById(id);
	}

	@Cacheable(cacheNames = "customer", key = "#id", unless = "#result == null")
	@Override
	public Customer getCustomerById(long id) {
		waitSomeTime();
		return this.customerRepository.findById(id).orElse(null);
	}

	private void waitSomeTime() {
		System.out.println("Inicio do consumo do BANCO");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Fim do consumo do BANCO");
	}

}
```

## Docker & Docker Compose


Dockerfile:

```
FROM openjdk:17
ADD ./target/spring-boot-redis-cache-0.0.1-SNAPSHOT.jar /usr/src/spring-boot-redis-cache-0.0.1-SNAPSHOT.jar
WORKDIR usr/src
ENTRYPOINT ["java","-jar", "spring-boot-redis-cache-0.0.1-SNAPSHOT.jar"]
```

Docker compose file:


docker-compose.yml

```yml
version: '3'

services:
  db:
    image: "postgres"
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 1203
  cache:
    image: "redis"
    ports: 
      - "6379:6379"
    environment:
      - ALLOW_EMPTY_PASSWORD=yes
      - REDIS_DISABLE_COMMANDS=FLUSHDB,FLUSHALL
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db/db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: 1203
      SPRING_REDIS_HOST: cache
      SPRING_REDIS_PORT: 6379
    depends_on:
      - db
      - cache
```

## Fazendo Build & Rodando a Aplicação

* Build Java Jar.

```shell
 $ mvn clean install
```

*  Docker Compose Build and Run

```shell
$ docker-compose build --no-cache
$ docker-compose up --force-recreate

```

Depois de executar o aplicativo, você pode executar no Browser `http://localhost:8080`.	

## Endpoints com Swagger


Você pode ver o endpoint na página `http://localhost:8080/swagger-ui.html`.
Eu usei Swagger para endpoints de visualização.





