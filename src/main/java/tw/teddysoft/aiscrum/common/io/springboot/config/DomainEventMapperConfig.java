package tw.teddysoft.aiscrum.common.io.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageDataMapper;
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Auto-registration of Domain Event TypeMappers using Spring classpath scanning.
 *
 * @see ADR-047: Domain Event Auto-Registration
 */
@Configuration
public class DomainEventMapperConfig {

    private static final Logger log = LoggerFactory.getLogger(DomainEventMapperConfig.class);
    private static final String BASE_PACKAGE_PATH = "classpath*:tw/teddysoft/aiscrum/**/*Events.class";

    @Bean(name = "domainEventTypeMapper")
    public DomainEventTypeMapper domainEventTypeMapper() {
        DomainEventTypeMapper globalMapper = DomainEventTypeMapper.create();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
            Resource[] resources = resolver.getResources(BASE_PACKAGE_PATH);

            int registeredCount = 0;
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }

                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();

                if (className.contains("$")) {
                    continue;
                }

                try {
                    Class<?> clazz = Class.forName(className);

                    if (!clazz.isInterface()) {
                        continue;
                    }

                    if (!InternalDomainEvent.class.isAssignableFrom(clazz)) {
                        continue;
                    }

                    Method mapperMethod = clazz.getMethod("mapper");
                    if (!Modifier.isStatic(mapperMethod.getModifiers())) {
                        continue;
                    }

                    DomainEventTypeMapper mapper = (DomainEventTypeMapper) mapperMethod.invoke(null);
                    mapper.getMap().forEach(globalMapper::put);
                    registeredCount++;

                    log.debug("Registered domain events from: {}", clazz.getSimpleName());

                } catch (ClassNotFoundException e) {
                    log.warn("Could not load class: {}", className);
                } catch (NoSuchMethodException e) {
                    // No mapper() method - skip this class
                }
            }

            log.info("Registered {} domain event types from {} Events interfaces",
                    globalMapper.getMap().size(), registeredCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to scan for domain event mappers", e);
        }

        MessageDataMapper.setMapper(globalMapper);
        DomainEventMapper.setMapper(globalMapper);

        return globalMapper;
    }
}
