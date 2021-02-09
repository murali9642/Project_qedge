/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.kafka.retrytopic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.SleepingBackOffPolicy;
import org.springframework.retry.support.RetrySynchronizationManager;

/**
 *
 * Generates the backoff values from the provided maxAttempts value and
 * {@link BackOffPolicy}.
 *
 * @author Tomaz Fernandes
 * @since 2.7.0
 *
 */
public class BackOffValuesGenerator {

	private static final BackOffPolicy DEFAULT_BACKOFF_POLICY = new FixedBackOffPolicy();
	/**
	 * Default number of times the message processing should be retried.
	 */
	public static final int DEFAULT_MAX_ATTEMPTS = 3;

	/**
	 * Constant to represent that the number of attempts is not set.
	 */
	public static final int NOT_SET = -1;

	private final int numberOfvaluesToCreate;
	private final BackOffPolicy backOffPolicy;

	public BackOffValuesGenerator(int providedMaxAttempts, BackOffPolicy providedBackOffPolicy) {
		this.numberOfvaluesToCreate = getMaxAttemps(providedMaxAttempts) - 1;
		BackOffPolicy backOffPolicy = providedBackOffPolicy != null ? providedBackOffPolicy : DEFAULT_BACKOFF_POLICY;
		checkBackOffPolicyTipe(backOffPolicy);
		this.backOffPolicy = backOffPolicy;
	}

	public int getMaxAttemps(int providedMaxAttempts) {
		return providedMaxAttempts != NOT_SET ? providedMaxAttempts : DEFAULT_MAX_ATTEMPTS;
	}

	public List<Long> generateValues() {
		return NoBackOffPolicy.class.isAssignableFrom(this.backOffPolicy.getClass())
				? generateFromNoBackOffPolicy(this.numberOfvaluesToCreate)
				: generateFromSleepingBackOffPolicy(this.numberOfvaluesToCreate, this.backOffPolicy);
	}

	private void checkBackOffPolicyTipe(BackOffPolicy providedBackOffPolicy) {
		if (!(SleepingBackOffPolicy.class.isAssignableFrom(providedBackOffPolicy.getClass())
				|| NoBackOffPolicy.class.isAssignableFrom(providedBackOffPolicy.getClass()))) {
			throw new IllegalArgumentException("Either a SleepingBackOffPolicy or a NoBackOffPolicy must be provided. " +
					"Provided BackOffPolicy: " + providedBackOffPolicy.getClass().getSimpleName());
		}
	}

	private List<Long> generateFromSleepingBackOffPolicy(int maxAttempts, BackOffPolicy providedBackOffPolicy) {
		BackoffRetainerSleeper sleeper = new BackoffRetainerSleeper();
		SleepingBackOffPolicy<?> retainingBackOffPolicy = ((SleepingBackOffPolicy<?>) providedBackOffPolicy).withSleeper(sleeper);
		BackOffContext backOffContext = retainingBackOffPolicy.start(RetrySynchronizationManager.getContext());
		IntStream.range(0, maxAttempts)
				.forEach(index -> retainingBackOffPolicy.backOff(backOffContext));

		return sleeper.getBackoffValues();
	}

	private List<Long> generateFromNoBackOffPolicy(int maxAttempts) {
		return LongStream
				.range(0, maxAttempts)
				.mapToObj(index -> 0L)
				.collect(Collectors.toList());
	}

	/**
	 * This class is injected in the backoff policy to gather and hold the generated backoff values.
	 */
	private static class BackoffRetainerSleeper implements Sleeper {

		private static final long serialVersionUID = 1L;
		private final List<Long> backoffValues = new ArrayList<>();

		@Override
		public void sleep(long backOffPeriod) throws InterruptedException {
			this.backoffValues.add(backOffPeriod);
		}

		public List<Long> getBackoffValues() {
			return this.backoffValues;
		}
	}
}
