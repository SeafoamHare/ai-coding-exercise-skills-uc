package tw.teddysoft.aiscrum.product.io.springboot.config;

import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.port.in.CreateProductUseCase;
import tw.teddysoft.aiscrum.product.usecase.service.CreateProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

@Configuration
public class ProductUseCaseConfig {

    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> productRepository) {
        return new CreateProductService(productRepository);
    }
}
