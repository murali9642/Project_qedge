/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.kafka.listener;

import javax.annotation.Nullable;

import org.springframework.core.NestedRuntimeException;
import org.springframework.core.log.LogAccessor;

/**
 * Internal {@link NestedRuntimeException} that is used as an exception thrown
 * when the record is in retry and not yet recovered during error handling.
 * This is to prevent the record from being prematurely committed in the middle of a retry.
 *
 * Intended only for framework use and thus the package-protected access.
 *
 * @author Soby Chacko
 * @since 3.3.0
 */
class RecordInRetryException extends NestedRuntimeException {

	private final String message;

	/**
	 * Package protected constructor to create an instance with the provided properties.
	 *
	 * @param message logging message
	 * @param cause {@link Throwable}
	 */
	RecordInRetryException(String message, @Nullable Throwable cause) {
		super(message, cause);
		this.message = message;
	}

	void selfLog(LogAccessor logger) {
		logger.info(this.message);
	}
}
