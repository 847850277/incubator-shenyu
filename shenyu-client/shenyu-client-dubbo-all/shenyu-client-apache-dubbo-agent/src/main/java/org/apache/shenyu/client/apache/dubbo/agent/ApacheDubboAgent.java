/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.client.apache.dubbo.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.shenyu.client.apache.dubbo.agent.transfer.GetApplicationListenersExitInterceptor;
import org.apache.shenyu.client.apache.dubbo.agent.transfer.RegisterBeanPostProcessorsEnterInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * The DubboAgent.
 */
public class ApacheDubboAgent {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheDubboAgent.class);

    /**
     * premain.
     *
     * @param arg             arg.
     * @param instrumentation instrumentation.
     */
    public static void premain(final String arg, final Instrumentation instrumentation) {

        AgentBuilder.Transformer transformer = new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder,
                                                    final TypeDescription typeDescription,
                                                    final ClassLoader classLoader,
                                                    final JavaModule module,
                                                    final ProtectionDomain protectionDomain) {
                try {
                    return builder
                            .method(ElementMatchers.named("registerBeanPostProcessors"))
                            .intercept(Advice.to(RegisterBeanPostProcessorsEnterInterceptor.class))
                            .method(ElementMatchers.named("getApplicationListeners"))
                            .intercept(Advice.to(GetApplicationListenersExitInterceptor.class));
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
                return builder;
            }
        };

        new AgentBuilder.Default()
                .type(ElementMatchers.named("org.springframework.context.support.AbstractApplicationContext"))
                .transform(transformer)
                .installOn(instrumentation);

    }
}
