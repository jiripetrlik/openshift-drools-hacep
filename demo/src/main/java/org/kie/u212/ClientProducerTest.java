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
package org.kie.u212;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.kie.u212.model.StockTickEvent;
import org.kie.u212.producer.ClientProducer;

public class ClientProducerTest {

    public static void main(String[] args) {
        insertBatchEvent(1);
    }

    private static void insertBatchEvent(int items) {
        EnvConfig envConfig = EnvConfig.getDefaultEnvConfig();
        Properties props = Config.getStatic();
        props.put("bootstrap.servers", "my-cluster-kafka-bootstrap-my-kafka-project.<ip>.nip.io:443");
        props.put("security.protocol", "SSL");
        props.put("ssl.keystore.location", "/<path>/openshift-drools-hacep/client/src/main/resources/keystore.jks");
        props.put("ssl.keystore.password", "password");
        props.put("ssl.truststore.location", "/<path>/openshift-drools-hacep/client/src/main/resources/keystore.jks");
        props.put("ssl.truststore.password", "password");

        ClientProducer producer = new ClientProducer(props, envConfig);
        try {
            for (int i = 0; i < items; i++) {
                StockTickEvent eventA = new StockTickEvent("RHT",
                                                           ThreadLocalRandom.current().nextLong(80,
                                                                                                100));
                producer.insertSync(eventA,
                                    true);
            }
        } finally {
            producer.stop();
        }
    }
}
