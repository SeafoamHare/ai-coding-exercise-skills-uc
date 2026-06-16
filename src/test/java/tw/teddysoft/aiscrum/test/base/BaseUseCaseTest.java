package tw.teddysoft.aiscrum.test.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import tw.teddysoft.aiscrum.common.NotifyFakeHandleAllEventsService;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.message.broker.adapter.in.consumer.InMemoryConsumer;
import tw.teddysoft.ezddd.message.broker.adapter.in.consumer.internal.InternalInMemoryMessageConsumer;
import tw.teddysoft.ezddd.message.broker.io.messagebroker.InMemoryMessageBroker;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Use Case tests with event capture support.
 *
 * Subclasses MUST call setUpEventCapture() in @BeforeEach
 * and tearDownEventCapture() in @AfterEach manually.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseUseCaseTest extends BaseSpringBootTest {

    @Autowired
    protected InMemoryMessageBroker<DomainEventData> inMemoryMessageBroker;

    @Autowired
    protected MessageProducer<DomainEventData> messageProducer;

    @Autowired(required = false)
    protected PgMessageDbClient pgMessageDbClient;

    @Autowired(required = false)
    protected JdbcTemplate jdbcTemplate;

    @Value("${spring.profiles.active:test-inmemory}")
    protected String activeProfile;

    protected NotifyFakeHandleAllEventsService notifyFakeHandleAllEventsService;
    protected InternalInMemoryMessageConsumer notifyHandleAllEventsConsumer;
    protected InMemoryConsumer<DomainEventData> inMemoryConsumer;
    protected ExecutorService executorService;

    protected void setUpEventCapture() {
        System.out.println("==> Running test with profile: " + activeProfile);

        if ("test-outbox".equals(activeProfile) && jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("DELETE FROM message_store.messages");
                jdbcTemplate.execute("ALTER SEQUENCE message_store.messages_global_position_seq RESTART WITH 1");
                System.out.println("Cleaned up message_store.messages table and reset sequence");
            } catch (Exception e) {
                System.err.println("Could not clean messages table: " + e.getMessage());
            }
        }

        notifyFakeHandleAllEventsService = new NotifyFakeHandleAllEventsService();

        inMemoryConsumer = new InMemoryConsumer<>(inMemoryMessageBroker);
        notifyHandleAllEventsConsumer = new InternalInMemoryMessageConsumer(
                notifyFakeHandleAllEventsService, inMemoryConsumer);
        executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        executorService.submit(notifyHandleAllEventsConsumer);

        if ("test-outbox".equals(activeProfile)) {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            notifyFakeHandleAllEventsService.clearHandledEvents();
        }
    }

    protected void tearDownEventCapture() {
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.out.println("ExecutorService did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void clearCapturedEvents() {
        if (notifyFakeHandleAllEventsService != null) {
            notifyFakeHandleAllEventsService.clearHandledEvents();
        }
    }
}
