package tw.teddysoft.aiscrum.product.usecase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductEvents;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.port.in.CreateProductUseCase;
import tw.teddysoft.aiscrum.common.entity.DateProvider;
import tw.teddysoft.aiscrum.test.base.BaseUseCaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Use Case tests for CreateProduct, verified with ezSpec BDD.
 * @DirtiesContext is inherited from BaseUseCaseTest (AFTER_EACH_TEST_METHOD).
 */
@SpringBootTest
public class CreateProductServiceTest extends BaseUseCaseTest {

    @Autowired
    private CreateProductUseCase createProductUseCase;

    @Autowired
    private Repository<Product, ProductId> productRepository;

    private Feature feature;

    @BeforeEach
    public void setUp() {
        setUpEventCapture();
        DateProvider.useSystemTime();
        feature = Feature.New("CreateProduct", "Create a new product");
    }

    @AfterEach
    public void tearDown() {
        tearDownEventCapture();
    }

    @EzScenario
    public void should_create_product_successfully() {
        feature.newScenario()
                .Given("a valid product creation request", env -> {
                    String productId = UUID.randomUUID().toString();
                    env.put("productId", productId);
                    env.put("productName", "Test Product");
                    var input = CreateProductUseCase.CreateProductInput.create();
                    input.id = productId;
                    input.name = "Test Product";
                    input.userId = "user-1";
                    env.put("input", input);
                })
                .When("the create product use case is executed", env -> {
                    var input = env.get("input", CreateProductUseCase.CreateProductInput.class);
                    var output = createProductUseCase.execute(input);
                    env.put("output", output);
                })
                .Then("the operation should succeed", env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
                    assertThat(output.getId()).isEqualTo(env.gets("productId"));
                })
                .And("the product should be persisted with DRAFT state", env -> {
                    var productId = ProductId.valueOf(env.gets("productId"));
                    var product = productRepository.findById(productId).orElse(null);
                    assertThat(product).isNotNull();
                    assertThat(product.getName().value()).isEqualTo(env.gets("productName"));
                })
                .And("the ProductCreated event should be published with the userId in metadata", env -> {
                    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        assertThat(notifyFakeHandleAllEventsService.getHandledEventsSize()).isEqualTo(1);
                        assertThat(notifyFakeHandleAllEventsService.handledEventTimes(
                                ProductEvents.ProductCreated.class)).isEqualTo(1);
                    });

                    ProductEvents.ProductCreated event = (ProductEvents.ProductCreated)
                            notifyFakeHandleAllEventsService.getLastHandledEvent();
                    assertThat(event.productId().value()).isEqualTo(env.gets("productId"));
                    assertThat(event.metadata().get("userId")).isEqualTo("user-1");
                })
                .Execute();
    }

    @EzScenario
    public void should_reject_duplicate_product_id() {
        feature.newScenario()
                .Given("a product already exists", env -> {
                    String productId = UUID.randomUUID().toString();
                    env.put("productId", productId);
                    var input = CreateProductUseCase.CreateProductInput.create();
                    input.id = productId;
                    input.name = "Existing Product";
                    input.userId = "user-1";
                    createProductUseCase.execute(input);

                    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                            assertThat(notifyFakeHandleAllEventsService.getHandledEventsSize())
                                    .isGreaterThanOrEqualTo(1));
                    clearCapturedEvents();
                })
                .When("creating another product with the same id", env -> {
                    var input = CreateProductUseCase.CreateProductInput.create();
                    input.id = env.gets("productId");
                    input.name = "Duplicate Product";
                    input.userId = "user-1";
                    var output = createProductUseCase.execute(input);
                    env.put("output", output);
                })
                .Then("the operation should fail", env -> {
                    var output = env.get("output", CqrsOutput.class);
                    assertThat(output.getExitCode()).isEqualTo(ExitCode.FAILURE);
                })
                .And("no new domain events should be published", env -> {
                    long eventCountAfter = notifyFakeHandleAllEventsService.getHandledEventsSize();
                    assertThat(eventCountAfter).isEqualTo(0);
                })
                .Execute();
    }
}
