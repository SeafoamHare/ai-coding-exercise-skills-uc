package tw.teddysoft.aiscrum.product.usecase.service;

import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.entity.ProductName;
import tw.teddysoft.aiscrum.product.usecase.port.in.CreateProductUseCase;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

import java.util.Objects;

import static tw.teddysoft.ucontract.Contract.*;

public class CreateProductService implements CreateProductUseCase {

    private final Repository<Product, ProductId> repository;

    public CreateProductService(Repository<Product, ProductId> repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public CqrsOutput<?> execute(CreateProductInput input) {
        requireNotNull("Input", input);
        requireNotNull("Product id", input.productId);
        requireNotNull("Product name", input.name);
        requireNotNull("User id", input.userId);

        try {
            var output = CqrsOutput.create();
            ProductId productId = ProductId.valueOf(input.productId);

            if (repository.findById(productId).isPresent()) {
                output.setId(input.productId)
                        .setExitCode(ExitCode.FAILURE)
                        .setMessage("Create product failed: product already exists, product id = " + input.productId);
                return output;
            }

            Product product = new Product(productId, ProductName.valueOf(input.name));
            repository.save(product);

            output.setId(input.productId).setExitCode(ExitCode.SUCCESS);
            return output;
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
