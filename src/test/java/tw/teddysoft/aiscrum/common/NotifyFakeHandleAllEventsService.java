package tw.teddysoft.aiscrum.common;

import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;

import java.util.ArrayList;
import java.util.List;

public class NotifyFakeHandleAllEventsService implements Reactor<DomainEventData> {
    private final List<InternalDomainEvent> handledDomainEvents = new ArrayList<>();

    @Override
    public void execute(DomainEventData message) {
        if (message != null) {
            this.handledDomainEvents.add(DomainEventMapper.toDomain(message));
        }
    }

    public long handledEventTimes(Class<?> clazz) {
        return handledDomainEvents.stream().filter(d -> d.getClass().isAssignableFrom(clazz)).count();
    }

    public int getHandledEventsSize() {
        return handledDomainEvents.size();
    }

    public InternalDomainEvent getLastHandledEvent() {
        return handledDomainEvents.getLast();
    }

    public List<InternalDomainEvent> getHandledEvents() {
        return handledDomainEvents;
    }

    public void clearHandledEvents() {
        handledDomainEvents.clear();
    }
}
