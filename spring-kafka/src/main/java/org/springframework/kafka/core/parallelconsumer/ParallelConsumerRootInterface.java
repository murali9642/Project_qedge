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

package org.springframework.kafka.core.parallelconsumer;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;

import io.confluent.parallelconsumer.internal.DrainingCloseable.DrainingMode;

/**
 * This interface provides a common interface for sub-interfaces.
 * Users should not implement this interface.
 * Users should implement one of {@link Poll}, {@link PollAndProduce}, {@link PollAndProduceResult}
 * , {@link PollAndProduceMany}, {@link PollAndProduceManyResult} instead of {@link ParallelConsumerRootInterface}.

 * @author Sanghyeok An
 *
 * @since 3.3
 */

public interface ParallelConsumerRootInterface<K, V> {

	/**
	 * ...
	 */
	List<String> getTopics();
	default Pattern getSubscribeTopicsPattern(){
		return null;
	}
	default ConsumerRebalanceListener getRebalanceListener(){
		return null;
	}
	default DrainingMode drainingMode() {
		return DrainingMode.DONT_DRAIN;
	}

	default Duration drainTimeOut() {
		return Duration.ofMillis(0);
	}

}
