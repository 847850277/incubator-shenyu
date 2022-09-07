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
import org.apache.shenyu.client.core.register.ShenyuClientRegisterRepositoryFactory;
import org.apache.shenyu.client.dubbo.agent.DubboAgent;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.client.http.HttpClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.config.ShenyuRegisterCenterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.EnvironmentPostProcessorApplicationListener;

import java.util.Collection;
import java.util.Properties;

/**
 * MethodInterceptor.
 */
public class AbstractApplicationContextInterceptor {


    /**
     * MethodInterceptor.
     *
     * @param listeners listeners.
     */
    @Advice.OnMethodExit
    public static void enter(@Advice.Return(readOnly=false, typing= Assigner.Typing.DYNAMIC) Collection listeners) {
//        System.out.println("argSize:" + args.length);
//        if(args.length > 0){
//            final Object arg = args[0];
//            System.out.println("class:" + arg.getClass());
//        }
        System.out.println(listeners.size());
        ShenyuRegisterCenterConfig registerCenterConfig = new ShenyuRegisterCenterConfig();
        registerCenterConfig.setRegisterType("http");
        registerCenterConfig.setServerLists("http://localhost:9095");
        Properties props = new Properties();
        props.setProperty("username","admin");
        props.setProperty("password","123456");
        registerCenterConfig.setProps(props);
        PropertiesConfig clientConfig = new PropertiesConfig();
        Properties clientProps = new Properties();
        clientProps.setProperty("appName","dubbo");
        clientProps.setProperty("contextPath","/dubbo");
        clientConfig.setProps(clientProps);
        ShenyuClientRegisterRepository shenyuClientRegisterRepository = new HttpClientRegisterRepository();
        AlibabaDubboServiceBeanListener alibabaDubboServiceBeanListener = new AlibabaDubboServiceBeanListener(registerCenterConfig,clientConfig,shenyuClientRegisterRepository);
        listeners.add(alibabaDubboServiceBeanListener);
        System.out.println(listeners.size());

    }

}
