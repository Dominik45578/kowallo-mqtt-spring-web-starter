package com.kowallo.spring.mqttwebstarter.handler;

import com.kowallo.spring.mqttwebstarter.annotation.MqttPublisher;
import com.kowallo.spring.mqttwebstarter.config.MqttPublisherFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;

public class MqttPublisherRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private final BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        if (!AutoConfigurationPackages.has(this.beanFactory)) {
            return;
        }

        List<String> basePackages = AutoConfigurationPackages.get(this.beanFactory);

        ClassPathScanningCandidateComponentProvider provider = getScannerProvider();
        provider.addIncludeFilter(new AnnotationTypeFilter(MqttPublisher.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = provider.findCandidateComponents(basePackage);

            for (BeanDefinition candidate : candidateComponents) {
                if (candidate instanceof AnnotatedBeanDefinition annotatedBeanDef) {
                    String beanClassName = annotatedBeanDef.getBeanClassName();
                    try {
                        Class<?> interfaceType = Class.forName(beanClassName);

                        // Rejestrujemy definicję, wskazując MqttPublisherFactoryBean jako fabrykę docelową
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MqttPublisherFactoryBean.class);
                        builder.addConstructorArgValue(interfaceType);
                        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

                        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
                        String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);

                        registry.registerBeanDefinition(beanName, beanDefinition);

                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Nie udało się zarejestrować proxy MqttPublisher dla klasy: " + beanClassName, e);
                    }
                }
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getScannerProvider() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                return metadata.isInterface() && metadata.isIndependent();
            }
        };
    }
}