package tw.teddysoft.aiscrum.product.usecase.port.out;

import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class ProductData implements OutboxData<String> {

    @Transient
    private List<DomainEventData> domainEventDatas;

    @Transient
    private String streamName;

    @Id
    @Column(name = "id")
    private String productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "goalJson", columnDefinition = "TEXT")
    private String goalJson;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "extension", columnDefinition = "TEXT")
    private String extension;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "isDeleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "lastUpdated", nullable = false)
    private Instant lastUpdated;

    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;

    public ProductData() {
        this(0L);
    }

    public ProductData(long version) {
        this.version = version;
        this.domainEventDatas = new ArrayList<>();
        this.isDeleted = false;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGoalJson() { return goalJson; }
    public void setGoalJson(String goalJson) { this.goalJson = goalJson; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    @Transient
    public String getId() { return productId; }

    @Override
    @Transient
    public void setId(String id) { this.productId = id; }

    @Override
    public long getVersion() { return version; }

    @Override
    public void setVersion(long version) { this.version = version; }

    @Override
    @Transient
    public List<DomainEventData> getDomainEventDatas() { return this.domainEventDatas; }

    @Override
    @Transient
    public void setDomainEventDatas(List<DomainEventData> domainEventDatas) { this.domainEventDatas = domainEventDatas; }

    @Override
    @Transient
    public String getStreamName() { return streamName; }

    @Override
    @Transient
    public void setStreamName(String streamName) { this.streamName = streamName; }
}
