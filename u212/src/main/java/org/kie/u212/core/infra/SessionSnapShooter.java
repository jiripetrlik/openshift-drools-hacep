/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.kie.u212.core.infra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.kie.api.KieServices;
import org.kie.api.marshalling.KieMarshallers;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.u212.Config;
import org.kie.u212.EnvConfig;
import org.kie.u212.core.infra.producer.EventProducer;
import org.kie.u212.core.infra.utils.RecordMetadataUtil;
import org.kie.u212.model.EventType;
import org.kie.u212.model.EventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionSnapShooter<T> {

    private final String key = "LAST-SNAPSHOT";
    private final Logger logger = LoggerFactory.getLogger(SessionSnapShooter.class);
    private EventProducer<Byte[]> producer;
    private KafkaConsumer<String, Byte[]> consumer;
    private KieContainer kieContainer;
    private EnvConfig envConfig;

    public SessionSnapShooter(EnvConfig envConfig) {
        this.envConfig = envConfig;
        KieServices srv = KieServices.get();
        if(srv != null){
            kieContainer = srv.newKieClasspathContainer();
            producer = new EventProducer<>();
            producer.start(Config.getSnapshotProducerConfig());
            configConsumer();
        }else{
            logger.error("KieServices is null");
        }
    }

    public void serialize(KieSession kSession, String lastInsertedEventkey, long lastInsertedEventOffset) {
        KieMarshallers marshallers = KieServices.get().getMarshallers();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            marshallers.newMarshaller(kSession.getKieBase()).marshall(out, kSession);
            /* We are storing the last inserted key and offset together with the session's bytes */
            byte[] bytez = out.toByteArray();
            EventWrapper wrapper = new EventWrapper(bytez,
                                                    lastInsertedEventkey,
                                                    0l,
                                                    EventType.SNAPSHOT,
                                                    lastInsertedEventOffset);
            RecordMetadata metadata = producer.produceSync(new ProducerRecord(envConfig.getSnapshotTopicName(), key, serializeEventWrapper(wrapper)));
            RecordMetadataUtil.logRecord(metadata);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private byte[] serializeEventWrapper(EventWrapper obj){
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }catch (IOException io){
            logger.error(io.getMessage(), io);
        }
        return new byte[]{};
    }

    private EventWrapper deserializeEventWrapper(byte[] bytes)  {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return (EventWrapper) o.readObject();
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        return new EventWrapper();
    }

    public SnapshotInfos deserializeEventWrapper() {
        KieServices srv = KieServices.get();
        if(srv != null) {
            KieMarshallers marshallers = KieServices.get().getMarshallers();
            KieSession kSession = null;
            ConsumerRecords<String, Byte[]> records = consumer.poll(Duration.of(Integer.valueOf(Config.DEFAULT_POLL_TIMEOUT_MS),
                                                                                ChronoUnit.MILLIS));
            EventWrapper wrapper = null;
            for (ConsumerRecord record : records) {
                //logger.info("snapshot record:{}", record);
                byte[] eventBytez = (byte[]) record.value();
                wrapper = deserializeEventWrapper(eventBytez);
            }
            if (wrapper != null && wrapper.getKey() != null) {
                try (ByteArrayInputStream in = new ByteArrayInputStream((byte[]) wrapper.getDomainEvent())) {
                    KieSessionConfiguration conf = KieServices.get().newKieSessionConfiguration();
                    conf.setOption(ClockTypeOption.get("pseudo"));
                    kSession = marshallers.newMarshaller(kieContainer.getKieBase()).unmarshall(in, conf, null);
                } catch (IOException | ClassNotFoundException e) {
                    logger.error(e.getMessage(),
                                 e);
                }
                return new SnapshotInfos(kSession,
                                         wrapper.getKey(),
                                         wrapper.getLongValueToStore());
            }
            return new SnapshotInfos();
        }else{
            return new SnapshotInfos();
        }
    }

    private void configConsumer() {
        consumer = new KafkaConsumer(Config.getSnapshotConsumerConfig());
        List<PartitionInfo> partitionsInfo = consumer.partitionsFor(envConfig.getSnapshotTopicName());
        List<TopicPartition> partitions = null;
        Collection<TopicPartition> partitionCollection = new ArrayList<>();

        if (partitionsInfo != null) {
            for (PartitionInfo partition : partitionsInfo) {
                if (partitions == null || partitions.contains(partition.partition())) {
                    partitionCollection.add(new TopicPartition(partition.topic(),
                                                               partition.partition()));
                }
            }
            if (!partitionCollection.isEmpty()) {
                consumer.assign(partitionCollection);
            }
        }
        consumer.assignment().forEach(topicPartition -> consumer.seekToBeginning(partitionCollection));
    }

    public void close() {
        producer.stop();
        consumer.close();
    }
}
