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

package org.apache.shenyu.client.dubbo.agent.transfer;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.model.bean.RegisterInfo;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import java.util.Optional;

/**
 * MethodInterceptor.
 */
public class RegisterBeanPostProcessorsEnterInterceptor {


    /**
     * MethodExitInterceptor.
     *
     * @param args args.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {

        for (Object arg : args) {
            if (arg instanceof ConfigurableListableBeanFactory) {
                ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) arg;
                Environment env = beanFactory.getBean(Environment.class);
                final String appName = env.getProperty(ShenyuClientConstants.SHENYU_CLIENT_DUBBO_PROPS_APPNAME);
                final String contextPath = env.getProperty(ShenyuClientConstants.SHENYU_CLIENT_DUBBO_PROPS_CONTEXTPATH);
                final String port = env.getProperty(ShenyuClientConstants.PORT);
                final String host = env.getProperty(ShenyuClientConstants.HOST);
                final String registerType = env.getProperty(ShenyuClientConstants.SHENYU_REGISTER_REGISTERTYPE);
                final String serverLists = env.getProperty(ShenyuClientConstants.SHENYU_REGISTER_SERVERLISTS);
                final String username = env.getProperty(ShenyuClientConstants.SHENYU_REGISTER_PROPS_USERNAME);
                final String password = env.getProperty(ShenyuClientConstants.SHENYU_REGISTER_PROPS_PASSWORD);
                Optional.ofNullable(appName).ifPresent(RegisterInfo::setAppName);
                Optional.ofNullable(contextPath).ifPresent(RegisterInfo::setContextPath);
                Optional.ofNullable(registerType).ifPresent(RegisterInfo::setRegisterType);
                Optional.ofNullable(serverLists).ifPresent(RegisterInfo::setServerLists);
                Optional.ofNullable(username).ifPresent(RegisterInfo::setUsername);
                Optional.ofNullable(password).ifPresent(RegisterInfo::setPassword);
                Optional.ofNullable(port).ifPresent(RegisterInfo::setPort);
                Optional.ofNullable(host).ifPresent(RegisterInfo::setHost);
            }
        }
    }
}
