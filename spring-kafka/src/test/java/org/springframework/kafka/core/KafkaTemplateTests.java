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

package org.springframework.kafka.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.kafka.test.assertj.KafkaConditions.key;
import static org.springframework.kafka.test.assertj.KafkaConditions.partition;
import static org.springframework.kafka.test.assertj.KafkaConditions.timestamp;
import static org.springframework.kafka.test.assertj.KafkaConditions.value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.ProducerListenerAdapter;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Igor Stepanov
 * @author Biju Kunjummen
 */
public class KafkaTemplateTests {

	private static final String INT_KEY_TOPIC = "intKeyTopic";

	private static final String STRING_KEY_TOPIC = "stringKeyTopic";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, INT_KEY_TOPIC, STRING_KEY_TOPIC);

	private static Consumer<Integer, String> consumer;

	@BeforeClass
	public static void setUp() throws Exception {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		consumer = cf.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, INT_KEY_TOPIC);
	}

	@AfterClass
	public static void tearDown() {
		consumer.close();
	}

	@Test
	public void testTemplate() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);

		template.setDefaultTopic(INT_KEY_TOPIC);

		template.sendDefault("foo");
		assertThat(KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC)).has(value("foo"));

		template.sendDefault(0, 2, "bar");
		ConsumerRecord<Integer, String> received = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(received).has(key(2));
		assertThat(received).has(partition(0));
		assertThat(received).has(value("bar"));

		template.send(INT_KEY_TOPIC, 0, 2, "baz");
		received = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(received).has(key(2));
		assertThat(received).has(partition(0));
		assertThat(received).has(value("baz"));

		template.send(INT_KEY_TOPIC, 0, null, "qux");
		received = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(received).has(key((Integer) null));
		assertThat(received).has(partition(0));
		assertThat(received).has(value("qux"));

		template.send(MessageBuilder.withPayload("fiz")
				.setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
				.setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader(KafkaHeaders.MESSAGE_KEY, 2)
				.build());
		received = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(received).has(key(2));
		assertThat(received).has(partition(0));
		assertThat(received).has(value("fiz"));

		template.send(MessageBuilder.withPayload("buz")
				.setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader(KafkaHeaders.MESSAGE_KEY, 2)
				.build());
		received = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(received).has(key(2));
		assertThat(received).has(partition(0));
		assertThat(received).has(value("buz"));

		Map<MetricName, ? extends Metric> metrics = template.execute(Producer::metrics);
		assertThat(metrics).isNotNull();
		metrics = template.metrics();
		assertThat(metrics).isNotNull();
		List<PartitionInfo> partitions = template.partitionsFor(INT_KEY_TOPIC);
		assertThat(partitions).isNotNull();
		assertThat(partitions.size()).isEqualTo(2);
		pf.destroy();
	}

	@Test
	public void testTemplateWithTimestamps() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);

		template.setDefaultTopic(INT_KEY_TOPIC);

		template.sendDefault(0, 1487694048607L, null, "foo-ts1");
		ConsumerRecord<Integer, String> r1 = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(r1).has(value("foo-ts1"));
		assertThat(r1).has(timestamp(1487694048607L));

		template.send(INT_KEY_TOPIC, 0, 1487694048610L, null, "foo-ts2");
		ConsumerRecord<Integer, String> r2 = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(r2).has(value("foo-ts2"));
		assertThat(r2).has(timestamp(1487694048610L));

		Map<MetricName, ? extends Metric> metrics = template.execute(Producer::metrics);
		assertThat(metrics).isNotNull();
		metrics = template.metrics();
		assertThat(metrics).isNotNull();
		List<PartitionInfo> partitions = template.partitionsFor(INT_KEY_TOPIC);
		assertThat(partitions).isNotNull();
		assertThat(partitions.size()).isEqualTo(2);
		pf.destroy();
	}

	@Test
	public void testWithMessage() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);

		Message<String> message1 = MessageBuilder.withPayload("foo-message")
				.setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
				.setHeader(KafkaHeaders.PARTITION_ID, 0)
				.build();

		template.send(message1);

		ConsumerRecord<Integer, String> r1 = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(r1).has(value("foo-message"));

		Message<String> message2 = MessageBuilder.withPayload("foo-message-2")
				.setHeader(KafkaHeaders.TOPIC, INT_KEY_TOPIC)
				.setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader(KafkaHeaders.TIMESTAMP, 1487694048615L)
				.build();

		template.send(message2);

		ConsumerRecord<Integer, String> r2 = KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		assertThat(r2).has(value("foo-message-2"));
		assertThat(r2).has(timestamp(1487694048615L));

		MessagingMessageConverter messageConverter = new MessagingMessageConverter();

		Message<?> recordToMessage = messageConverter.toMessage(r2, null, String.class);

		assertThat(recordToMessage.getHeaders().get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
		assertThat(recordToMessage.getHeaders().get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048615L);
		assertThat(recordToMessage.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(INT_KEY_TOPIC);
		assertThat(recordToMessage.getPayload()).isEqualTo("foo-message-2");

		pf.destroy();
	}

	@Test
	public void withListener() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(INT_KEY_TOPIC);
		final CountDownLatch latch = new CountDownLatch(1);
		template.setProducerListener(new ProducerListenerAdapter<Integer, String>() {

			@Override
			public void onSuccess(String topic, Integer partition, Integer key, String value,
					RecordMetadata recordMetadata) {
				latch.countDown();
			}

			@Override
			public boolean isInterestedInSuccess() {
				return true;
			}

		});
		template.sendDefault("foo");
		template.flush();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		//Drain the topic
		KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC);
		pf.destroy();
	}

	@Test
	public void testWithCallback() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);
		template.setDefaultTopic(INT_KEY_TOPIC);
		ListenableFuture<SendResult<Integer, String>> future = template.sendDefault("foo");
		template.flush();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<SendResult<Integer, String>> theResult = new AtomicReference<>();
		future.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {

			@Override
			public void onSuccess(SendResult<Integer, String> result) {
				theResult.set(result);
				latch.countDown();
			}

			@Override
			public void onFailure(Throwable ex) {
			}

		});
		assertThat(KafkaTestUtils.getSingleRecord(consumer, INT_KEY_TOPIC)).has(value("foo"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		pf.createProducer().close();
	}

	@Test
	public void testTemplateDisambiguation() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		pf.setKeySerializer(new StringSerializer());
		KafkaTemplate<String, String> template = new KafkaTemplate<>(pf, true);
		template.setDefaultTopic(STRING_KEY_TOPIC);
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testTString", "false", embeddedKafka);
		DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		cf.setKeyDeserializer(new StringDeserializer());
		Consumer<String, String> consumer = cf.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, STRING_KEY_TOPIC);
		template.sendDefault("foo", "bar");
		template.flush();
		ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, STRING_KEY_TOPIC);
		assertThat(record).has(Assertions.<ConsumerRecord<String, String>>allOf(key("foo"), value("bar")));
		consumer.close();
		pf.createProducer().close();
	}

}
