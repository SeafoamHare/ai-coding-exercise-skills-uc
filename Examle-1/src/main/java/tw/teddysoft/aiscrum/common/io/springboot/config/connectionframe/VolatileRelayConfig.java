package tw.teddysoft.aiscrum.common.io.springboot.config.connectionframe;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.ezddd.data.EzesVolatileRelay;
import tw.teddysoft.ezddd.data.adapter.ezes.out.MessageDbToDomainEventDataConverter;
import tw.teddysoft.ezddd.data.io.ezes.store.InMemoryMessageDbClient;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryMessageProducer;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryProducer;
import tw.teddysoft.ezddd.message.broker.io.messagebroker.InMemoryMessageBroker;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.util.concurrent.ExecutorService;

@Configuration
@Profile({"inmemory", "test-inmemory"})
public class VolatileRelayConfig {

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
    public EzesVolatileRelay<DomainEventData> ezesVolatileRelay(
            InMemoryMessageDbClient messageDbClient,
            MessageProducer<DomainEventData> messageProducer,
            ExecutorService relayExecutor) {
        EzesVolatileRelay.RelayConfiguration<DomainEventData> configuration =
                EzesVolatileRelay.RelayConfiguration.of(
                        messageDbClient,
                        messageProducer,
                        new MessageDbToDomainEventDataConverter());
        EzesVolatileRelay<DomainEventData> relay = new EzesVolatileRelay<>(configuration);
        relayExecutor.execute(relay);
        return relay;
    }
}
