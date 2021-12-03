import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.redis.client.Command;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class VertxConfigRedisTest {

  private final String configuration = "some-configuration";
  private final String configurationName = "name";
  private final String configurationValue = "value";

  @Container
  GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
      .withExposedPorts(6379);

  String host;
  Integer port;

  @BeforeEach
  void setUp() {
    host = redis.getHost();
    port = redis.getFirstMappedPort();
  }

  @Test
  void it_can_read_a_config_setting_from_redis() {

    var vertx = Vertx.vertx();

    configureValueInRedis(vertx);

    ConfigStoreOptions store = new ConfigStoreOptions()
        .setType("redis")
        .setConfig(new JsonObject()
            .put("host", host)
            .put("port", port)
            .put("key", configuration)
        );

    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(store);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    var config = retriever.rxGetConfig().blockingGet();

    assertEquals(configurationValue, config.getString(configurationName));
  }

  private void configureValueInRedis(Vertx vertx) {
    var client = Redis.createClient(
            vertx,
            "redis://" + host +":" + port)
        .rxConnect()
        .blockingGet();

    client
        .rxSend(
            Request.cmd(Command.HSET)
                .arg(configuration)
                .arg(configurationName)
                .arg(configurationValue)
        ).ignoreElement().blockingAwait();
  }
}
