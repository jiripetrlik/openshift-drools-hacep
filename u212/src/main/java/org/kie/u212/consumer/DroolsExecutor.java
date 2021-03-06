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

package org.kie.u212.consumer;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DroolsExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DroolsExecutor.class);

    private static boolean isMaster;

    protected Queue<Object> executionResults = new ArrayDeque<>();

    public static DroolsExecutor getInstance() {
        return isMaster ? Master.INSTANCE : Slave.INSTANCE;
    }

    public static void setAsMaster() {
        isMaster = true;
    }

    public static void setAsSlave() { isMaster = false; }

    public abstract void execute( Runnable f );

    public abstract <R> R execute( Supplier<R> f );

    public Queue<Object> getAndReset() {
        throw new UnsupportedOperationException();
    }

    public void setResult(Queue<Object> results) {
        throw new UnsupportedOperationException();
    }

    public static class Master extends DroolsExecutor {

        private static final Master INSTANCE = new Master();

        @Override
        public void execute( Runnable f ) {
            f.run();
            executionResults.add( EmptyResult.INSTANCE );
        }

        @Override
        public <R> R execute( Supplier<R> f ) {
            R result = f.get();
            executionResults.add( result );
            return result;
        }

        @Override
        public Queue<Object> getAndReset() {
            Queue<Object> results = executionResults;
            executionResults = new ArrayDeque<>();
            return results;
        }
    }

    public static class Slave extends DroolsExecutor {

        private static final Slave INSTANCE = new Slave();

        @Override
        public void execute( Runnable f ) {
            executionResults.poll();
        }

        @Override
        public <R> R execute( Supplier<R> f ) {
            return ( R ) executionResults.poll();
        }

        @Override
        public void setResult(Queue<Object> results) {
            executionResults = results;
        }
    }

    private static class EmptyResult implements Serializable {
        private static final EmptyResult INSTANCE = new EmptyResult();
    }
}
