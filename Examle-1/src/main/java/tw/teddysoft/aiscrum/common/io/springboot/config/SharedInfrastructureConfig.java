package tw.teddysoft.aiscrum.common.io.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.data.io.ezes.store.InMemoryMessageDb;
import tw.teddysoft.ezddd.data.io.ezes.store.InMemoryMessageDbClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile({"inmemory", "test-inmemory"})
public class SharedInfrastructureConfig {

    @Bean
    public InMemoryMessageDb inMemoryMessageDb() {
        return new InMemoryMessageDb();
    }

    @Bean
    public InMemoryMessageDbClient inMemoryMessageDbClient(InMemoryMessageDb messageDb) {
        return new InMemoryMessageDbClient(messageDb);
    }

    @Bean
    public ExecutorService relayExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    }
}
