package tw.teddysoft.aiscrum.product.io.springboot.config;

import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.port.ProductMapper;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeer;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezes.store.InMemoryMessageDbClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter;
import tw.teddysoft.ezddd.data.io.ezoutbox.InMemoryOrmClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.InMemoryOrmDb;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Profile({"inmemory", "test-inmemory"})
public class ProductInMemoryRepositoryConfig {

    @Bean
    public Map<String, ProductData> productDataStore() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public InMemoryOrmDb<ProductData> productOrmDb(
            Map<String, ProductData> productDataStore) {
        return new InMemoryOrmDb<>(productDataStore);
    }

    @Bean("productRepository")
    public Repository<Product, ProductId> productRepository(
            InMemoryOrmDb<ProductData> ormDb,
            InMemoryMessageDbClient messageDbClient) {

        InMemoryOrmClient<ProductData> ormClient = new InMemoryOrmClient<>(ormDb);
        EzOutboxClient<ProductData, String> outboxClient =
                new EzOutboxClient<>(ormClient, messageDbClient);
        OutboxStore<ProductData, String> outboxStore =
                EzOutboxStoreAdapter.createOutboxStore(outboxClient);
        OutboxRepositoryPeer<ProductData, String> peer =
                new OutboxRepositoryPeer<>(outboxStore);

        return new OutboxRepository<>(peer, ProductMapper.newMapper());
    }
}
