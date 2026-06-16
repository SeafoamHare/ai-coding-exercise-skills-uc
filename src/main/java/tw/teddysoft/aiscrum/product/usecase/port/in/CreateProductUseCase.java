package tw.teddysoft.aiscrum.product.usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface CreateProductUseCase extends Command<CreateProductUseCase.CreateProductInput, CqrsOutput<?>> {

    class CreateProductInput implements Input {
        public String id;
        public String name;
        public String userId;

        public static CreateProductInput create() {
            return new CreateProductInput();
        }
    }
}
