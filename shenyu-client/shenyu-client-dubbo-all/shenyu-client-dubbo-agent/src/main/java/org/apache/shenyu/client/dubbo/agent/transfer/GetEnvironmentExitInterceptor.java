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
import org.apache.shenyu.client.alibaba.dubbo.AlibabaDubboServiceBeanListener;
import org.apache.shenyu.client.dubbo.agent.model.bean.RegisterBeanInfo;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.client.consul.ConsulClientRegisterRepository;
import org.apache.shenyu.register.client.etcd.EtcdClientRegisterRepository;
import org.apache.shenyu.register.client.http.HttpClientRegisterRepository;
import org.apache.shenyu.register.client.nacos.NacosClientRegisterRepository;
import org.apache.shenyu.register.client.zookeeper.ZookeeperClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.config.ShenyuRegisterCenterConfig;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

/**
 * MethodInterceptor.
 */
public class GetEnvironmentExitInterceptor {


    /**
     * MethodExitInterceptor.
     *
     * @param environment environment.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Return(readOnly=false, typing= Assigner.Typing.DYNAMIC) ConfigurableEnvironment environment) {
        Environment env = (Environment) environment;
        final String appName = env.getProperty("shenyu.client.dubbo.props.appName");
        final String contextPath = env.getProperty("shenyu.client.dubbo.props.contextPath");
        final String port = env.getProperty("port");
        final String host = env.getProperty("host");
        final String registerType = env.getProperty("shenyu.register.registerType");
        final String serverLists = env.getProperty("shenyu.register.serverLists");
        final String username = env.getProperty("shenyu.register.props.username");
        final String password = env.getProperty("shenyu.register.props.password");

        Optional.ofNullable(appName).ifPresent(RegisterBeanInfo::setAppName);
        Optional.ofNullable(contextPath).ifPresent(RegisterBeanInfo::setContextPath);
        Optional.ofNullable(registerType).ifPresent(RegisterBeanInfo::setRegisterType);
        Optional.ofNullable(serverLists).ifPresent(RegisterBeanInfo::setServerLists);
        Optional.ofNullable(username).ifPresent(RegisterBeanInfo::setUsername);
        Optional.ofNullable(password).ifPresent(RegisterBeanInfo::setPassword);
        Optional.ofNullable(port).ifPresent(RegisterBeanInfo::setPort);
        Optional.ofNullable(host).ifPresent(RegisterBeanInfo::setHost);

    }

}
