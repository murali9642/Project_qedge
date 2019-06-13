/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.kafka.listener.reactive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.annotation.KafkaListener;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * Adapter for {@link KafkaListener} with a {@link Flux} parameter.
 *
 * @author Gary Russell
 * @since 2.3
 *
 */
public class ReactorAdapter implements SmartLifecycle {

	private final Object bean;

	private final Method method;

	private final List<String> topics;

	private final Map<String, Object> configs = new HashMap<>();

	private Disposable disposable;

	private volatile boolean running;

	public ReactorAdapter(Object bean, Method method, String... topics) {
		this.bean = bean;
		this.method = method;
		this.method.setAccessible(true);
		this.topics = Arrays.asList(topics);
	}

	public void setConfigs(Map<String, Object> configs) {
		this.configs.putAll(configs);
	}

	@Override
	public void start() {
		ReceiverOptions<?, ?> options = ReceiverOptions.create(this.configs)
				.subscription(this.topics);
		Flux<?> flux = KafkaReceiver.create(options).receive();
		/*
		 *  If the method's parameter is not a ReceiverRecord, we could map
		 *  record.value() using Spring's ConversionService.
		 */
		try {
			Object result = this.method.invoke(this.bean, flux);
			if (result instanceof Disposable) {
				this.disposable = (Disposable) result;
			}
			this.running = true;
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		if (this.disposable != null && !this.disposable.isDisposed()) {
			this.disposable.dispose();
			this.disposable = null;
		}
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
