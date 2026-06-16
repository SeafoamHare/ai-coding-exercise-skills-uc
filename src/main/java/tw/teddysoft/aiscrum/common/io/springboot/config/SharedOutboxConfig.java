package tw.teddysoft.aiscrum.common.io.springboot.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;

/**
 * Shared Outbox configuration for PostgreSQL persistence.
 */
@Configuration
@Profile({"outbox", "test-outbox"})
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
public class SharedOutboxConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
}
