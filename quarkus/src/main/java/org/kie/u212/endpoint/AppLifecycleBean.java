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
package org.kie.u212.endpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.kie.u212.Config;
import org.kie.u212.EnvConfig;
import org.kie.u212.core.Bootstrap;
import org.kie.u212.core.infra.utils.PrinterLogImpl;

@ApplicationScoped
public class AppLifecycleBean {

    void onStart(@Observes StartupEvent ev) {
        Bootstrap.startEngine(new PrinterLogImpl(), EnvConfig.getDefaultEnvConfig());
    }

    void onStop(@Observes ShutdownEvent ev) {
        Bootstrap.stopEngine();
    }
}