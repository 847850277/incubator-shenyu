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
import org.apache.shenyu.client.dubbo.agent.contant.AgentConstant;
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

import java.util.Collection;
import java.util.Properties;

/**
 * MethodInterceptor.
 */
public class GetApplicationListenersExitInterceptor {


    /**
     * MethodExitInterceptor.
     *
     * @param listeners listeners.
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Return(readOnly=false, typing= Assigner.Typing.DYNAMIC) Collection<ApplicationListener<?>> listeners) {
        final String registerType = RegisterBeanInfo.getRegisterType();
        final String serverLists = RegisterBeanInfo.getServerLists();
        final String username = RegisterBeanInfo.getUsername();
        final String password = RegisterBeanInfo.getPassword();
        final String appName = RegisterBeanInfo.getAppName();
        final String contextPath = RegisterBeanInfo.getContextPath();
        RegisterBeanInfo.validateNull();
        ShenyuRegisterCenterConfig registerCenterConfig = new ShenyuRegisterCenterConfig();
        registerCenterConfig.setRegisterType(registerType);
        registerCenterConfig.setServerLists(serverLists);
        Properties props = new Properties();
        props.setProperty(AgentConstant.USERNAME,username);
        props.setProperty(AgentConstant.PASSWORD,password);
        registerCenterConfig.setProps(props);
        PropertiesConfig clientConfig = new PropertiesConfig();
        Properties clientProps = new Properties();
        clientProps.setProperty(AgentConstant.APPNAME,appName);
        clientProps.setProperty(AgentConstant.CONTEXTPATH,contextPath);
        clientConfig.setProps(clientProps);
        ShenyuClientRegisterRepository shenyuClientRegisterRepository = null;
        switch (registerType){
            case "http":
                shenyuClientRegisterRepository = new HttpClientRegisterRepository();
                break;
            case "zookeeper":
                shenyuClientRegisterRepository = new ZookeeperClientRegisterRepository();
                break;
            case "etcd":
                shenyuClientRegisterRepository = new EtcdClientRegisterRepository();
                break;
            case "nacos":
                shenyuClientRegisterRepository = new NacosClientRegisterRepository();
                break;
            case "consul":
                shenyuClientRegisterRepository = new ConsulClientRegisterRepository();
                break;
            default:
                throw new IllegalStateException("Unknown register type: " + registerType);
        }
        AlibabaDubboServiceBeanListener alibabaDubboServiceBeanListener = new AlibabaDubboServiceBeanListener(registerCenterConfig,clientConfig,shenyuClientRegisterRepository);
        listeners.add(alibabaDubboServiceBeanListener);

    }

}
