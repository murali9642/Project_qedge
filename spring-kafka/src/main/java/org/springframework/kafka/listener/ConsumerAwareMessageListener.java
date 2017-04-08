/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.kafka.listener;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.kafka.support.Acknowledgment;

/**
 * Listener for handling individual incoming Kafka messages.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Gary Russell
 */
@FunctionalInterface
public interface ConsumerAwareMessageListener<K, V> extends AcknowledgingMessageListener<K, V> {

	/**
	 * Invoked with data from kafka. Containers should never call this since it they
	 * will detect we are a consumer aware acknowledging listener.
	 * @param data the data to be processed.
	 * @param acknowledgment the acknowledgment.
	 */
	@Override
	default void onMessage(ConsumerRecord<K, V> data, Acknowledgment acknowledgment) {
		System.out.println(getClass().getName());
		throw new UnsupportedOperationException("Container should never call this");
	}

	@Override
	void onMessage(ConsumerRecord<K, V> data, Consumer<?, ?> consumer);

}
