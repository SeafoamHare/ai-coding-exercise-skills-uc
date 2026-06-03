package tw.teddysoft.aiscrum.common.io.springboot.config.connectionframe;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.data.EzesCatchUpRelay;
import tw.teddysoft.ezddd.data.adapter.ezes.out.MessageDbToDomainEventDataConverter;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryMessageProducer;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryProducer;
import tw.teddysoft.ezddd.message.broker.io.messagebroker.InMemoryMessageBroker;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile({"outbox", "test-outbox"})
public class CatchupRelayConfig {

    @Bean
    public InMemoryMessageBroker<DomainEventData> inMemoryMessageBroker() {
        return new InMemoryMessageBroker<>();
    }

    @Bean
    public InMemoryProducer<DomainEventData> inMemoryProducer(
            InMemoryMessageBroker<DomainEventData> inMemoryMessageBroker) {
        return new InMemoryProducer<>(inMemoryMessageBroker);
    }

    @Bean
    public MessageProducer<DomainEventData> messageProducer(
            InMemoryProducer<DomainEventData> inMemoryProducer) {
        return InMemoryMessageProducer.internal(inMemoryProducer);
    }

    @Bean
    public EzesCatchUpRelay<DomainEventData> ezesCatchUpRelay(
            PgMessageDbClient pgMessageDbClient,
            MessageProducer<DomainEventData> messageProducer) {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        Path checkpointPath = Path.of(System.getProperty("java.io.tmpdir"),
                "relay-checkpoint-" + UUID.randomUUID());
        EzesCatchUpRelay.RelayConfiguration<DomainEventData> configuration =
                EzesCatchUpRelay.RelayConfiguration.of(
                        pgMessageDbClient,
                        messageProducer,
                        checkpointPath,
                        new MessageDbToDomainEventDataConverter());
        EzesCatchUpRelay<DomainEventData> relay = new EzesCatchUpRelay<>(configuration);
        executor.execute(relay);
        return relay;
    }
}
