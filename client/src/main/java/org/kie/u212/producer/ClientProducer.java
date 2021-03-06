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
package org.kie.u212.producer;

import java.io.Closeable;
import java.util.Properties;

import org.kie.u212.EnvConfig;

public class ClientProducer implements Closeable {

    private Sender sender;

    public ClientProducer(Properties configuration, EnvConfig envConfig) {
        sender = new Sender(configuration, envConfig);
        sender.start();
    }

    public void stop(){
        sender.stop();
    }

    public void insertSync(Object obj, boolean logInsert) {
        sender.insertSync(obj, logInsert);
    }

    @Override
    public void close() {
        sender.stop();
    }
}