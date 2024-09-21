/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.parallelconsumer.ParallelConsumerRootInterface;
import org.springframework.kafka.core.ParallelConsumerFactory;
import org.springframework.kafka.annotation.EnableParallelConsumer;
import org.springframework.kafka.core.parallelconsumer.ParallelConsumerOptionsProvider;

/**
 * If User decide to use parallelConsumer on SpringKafka, User should import this class to their ComponentScan scopes.
 * If so, this class will register both {@link ParallelConsumerContext} and {@link ParallelConsumerFactory} as Spring Bean.
 * User has responsibility
 *   1. annotated {@link EnableParallelConsumer} on their spring application
 *   2. register ConcreteClass of {@link ParallelConsumerRootInterface}.
 *
 * @author Sanghyoek An
 *
 * @since 3.3
 */

public class ParallelConsumerConfiguration<K, V> {

	@Bean
	@Conditional(OnMissingParallelConsumerOptionsProviderCondition.class)
	public ParallelConsumerOptionsProvider<K, V> parallelConsumerOptionsProvider() {
		return new ParallelConsumerOptionsProvider<K, V>() {};
	}

	@Bean(name = ParallelConsumerConfig.DEFAULT_BEAN_NAME)
	public ParallelConsumerConfig<K, V> parallelConsumerConfig(ParallelConsumerOptionsProvider<K, V> parallelConsumerOptionsProvider) {
		return new ParallelConsumerConfig<K, V>(parallelConsumerOptionsProvider);
	}

	@Bean(name = ParallelConsumerContext.DEFAULT_BEAN_NAME)
	public ParallelConsumerContext<K,V> parallelConsumerContext(ParallelConsumerConfig<K, V> parallelConsumerConfig,
																ParallelConsumerRootInterface<K, V> parallelConsumerCallback) {
		return new ParallelConsumerContext<K, V>(parallelConsumerConfig,
												 parallelConsumerCallback);
	}

	@Bean(name = ParallelConsumerFactory.DEFAULT_BEAN_NAME)
	public ParallelConsumerFactory<K,V> parallelConsumerFactory(DefaultKafkaConsumerFactory<K,V> consumerFactory,
																DefaultKafkaProducerFactory<K,V> producerFactory,
																ParallelConsumerContext<K,V> parallelConsumerContext) {
		return new ParallelConsumerFactory<K, V>(parallelConsumerContext, consumerFactory, producerFactory);
	}

}
