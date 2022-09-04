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

package org.apache.shenyu.client.dubbo.agent.bean;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.apache.shenyu.client.core.disruptor.ShenyuClientRegisterEventPublisher;
import org.apache.shenyu.client.core.register.ShenyuClientRegisterRepositoryFactory;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.apache.shenyu.client.dubbo.common.dto.DubboRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.client.http.HttpClientRegisterRepository;
import org.apache.shenyu.register.common.config.ShenyuRegisterCenterConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * The type Spring cloud client event listener.
 */
@Component
public class SpringCloudClientEventListener implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * api path separator.
     */
    private static final String PATH_SEPARATOR = "/";

    private static final Logger LOG = LoggerFactory.getLogger(SpringCloudClientEventListener.class);

    private final ShenyuClientRegisterEventPublisher publisher = ShenyuClientRegisterEventPublisher.getInstance();

    private String contextPath;

    private String appName;

    private String host;

    private String port;

    /**
     * constructor .
     */
    public SpringCloudClientEventListener() {

    }

    /**
     * getContextPath.
     *
     * @return getContextPath.
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * setContextPath.
     *
     * @param contextPath contextPath.
     */
    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * getAppName.
     *
     * @return appName.
     */
    public String getAppName() {
        return appName;
    }

    /**
     * setAppName.
     *
     * @param appName appName.
     */
    public void setAppName(final String appName) {
        this.appName = appName;
    }

    /**
     * getHost.
     *
     * @return String.
     */
    public String getHost() {
        return host;
    }

    /**
     * setHost.
     *
     * @param host host.
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * getPort.
     *
     * @return port.
     */
    public String getPort() {
        return port;
    }

    /**
     * setPort.
     *
     * @param port port.
     */
    public void setPort(final String port) {
        this.port = port;
    }

    /**
     * onApplicationEvent .
     *
     * @param contextRefreshedEvent .
     */
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        final Environment bean1 = contextRefreshedEvent.getApplicationContext().getBean(Environment.class);
        final String appName = bean1.getProperty("shenyu.client.dubbo.props.appName");
        final String contextPath = bean1.getProperty("shenyu.client.dubbo.props.contextPath");
        final String port = bean1.getProperty("port");
        final String host = bean1.getProperty("host");
        this.setAppName(appName);
        this.setContextPath(contextPath);
        this.setPort(port);
        this.setHost(host);
        final String registerType = bean1.getProperty("shenyu.register.registerType");
        final String serverLists = bean1.getProperty("shenyu.register.serverLists");
        final String username = bean1.getProperty("shenyu.register.props.username");
        final String password = bean1.getProperty("shenyu.register.props.password");
        ShenyuRegisterCenterConfig config = new ShenyuRegisterCenterConfig();
        Properties props = new Properties();
        config.setServerLists(serverLists);
        config.setRegisterType(registerType);
        props.put("username", username);
        props.put("password", password);
        config.setProps(props);
        ShenyuClientRegisterRepository registerRepository = new HttpClientRegisterRepository();
        registerRepository.init(config);
        publisher.start(registerRepository);
        ShenyuClientRegisterRepositoryFactory.newInstance(config);
        LOG.info("init SpringCloudClientEventListener:" + contextRefreshedEvent);
        Map<String, ServiceBean> serviceBean = contextRefreshedEvent.getApplicationContext().getBeansOfType(ServiceBean.class);
        for (Map.Entry<String, ServiceBean> entry : serviceBean.entrySet()) {
            handler(entry.getValue());
        }
        serviceBean.values()
                .stream()
                .findFirst()
                .ifPresent(bean -> publisher.publishEvent(buildURIRegisterDTO(bean)));
    }

    private void handler(final ServiceBean<?> serviceBean) {
        Object refProxy = serviceBean.getRef();
        Class<?> clazz = refProxy.getClass();
        if (AopUtils.isAopProxy(refProxy)) {
            clazz = AopUtils.getTargetClass(refProxy);
        }
        ShenyuDubboClient beanShenyuClient = AnnotatedElementUtils.findMergedAnnotation(clazz, ShenyuDubboClient.class);
        final String superPath = buildApiSuperPath(beanShenyuClient);
        if (superPath.contains("*") && Objects.nonNull(beanShenyuClient)) {
            Method[] methods = ReflectionUtils.getDeclaredMethods(clazz);
            for (Method method : methods) {
                publisher.publishEvent(buildMetaDataDTO(serviceBean, beanShenyuClient, method, superPath));
            }
            return;
        }
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        for (Method method : methods) {
            ShenyuDubboClient shenyuDubboClient = AnnotatedElementUtils.findMergedAnnotation(method, ShenyuDubboClient.class);
            if (Objects.nonNull(shenyuDubboClient)) {
                publisher.publishEvent(buildMetaDataDTO(serviceBean, shenyuDubboClient, method, superPath));
            }
        }
    }

    private MetaDataRegisterDTO buildMetaDataDTO(final ServiceBean<?> serviceBean, final ShenyuDubboClient shenyuDubboClient, final Method method, final String superPath) {
        String path = superPath.contains("*") ? pathJoin(getContextPath(), superPath.replace("*", ""), method.getName()) : pathJoin(getContextPath(), superPath, shenyuDubboClient.path());
        String desc = shenyuDubboClient.desc();
        String serviceName = serviceBean.getInterface();
        String configRuleName = shenyuDubboClient.ruleName();
        String ruleName = ("".equals(configRuleName)) ? path : configRuleName;
        String methodName = method.getName();
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName).collect(Collectors.joining(","));
        return MetaDataRegisterDTO.builder()
                .appName(buildAppName(serviceBean))
                .serviceName(serviceName)
                .methodName(methodName)
                .contextPath(getContextPath())
                .host(buildHost())
                .port(buildPort(serviceBean))
                .path(path)
                .ruleName(ruleName)
                .pathDesc(desc)
                .parameterTypes(parameterTypes)
                .rpcExt(buildRpcExt(serviceBean))
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .enabled(shenyuDubboClient.enabled())
                .build();
    }

    private String buildApiSuperPath(final ShenyuDubboClient shenyuDubboClient) {
        if (Objects.nonNull(shenyuDubboClient) && !StringUtils.isBlank(shenyuDubboClient.path())) {
            return shenyuDubboClient.path();
        }
        return "";
    }

    private String pathJoin(@NonNull final String... path) {
        StringBuilder result = new StringBuilder(PATH_SEPARATOR);
        for (String p : path) {
            if (!result.toString().endsWith(PATH_SEPARATOR)) {
                result.append(PATH_SEPARATOR);
            }
            result.append(p.startsWith(PATH_SEPARATOR) ? p.replaceFirst(PATH_SEPARATOR, "") : p);
        }
        return result.toString();
    }

    private String buildRpcExt(final ServiceBean<?> serviceBean) {
        DubboRpcExt builder = DubboRpcExt.builder()
                .group(StringUtils.isNotEmpty(serviceBean.getGroup()) ? serviceBean.getGroup() : "")
                .version(StringUtils.isNotEmpty(serviceBean.getVersion()) ? serviceBean.getVersion() : "")
                .loadbalance(StringUtils.isNotEmpty(serviceBean.getLoadbalance()) ? serviceBean.getLoadbalance() : Constants.DEFAULT_LOADBALANCE)
                .retries(Objects.isNull(serviceBean.getRetries()) ? Constants.DEFAULT_RETRIES : serviceBean.getRetries())
                .timeout(Objects.isNull(serviceBean.getTimeout()) ? Constants.DEFAULT_CONNECT_TIMEOUT : serviceBean.getTimeout())
                .sent(Objects.isNull(serviceBean.getSent()) ? Constants.DEFAULT_SENT : serviceBean.getSent())
                .cluster(StringUtils.isNotEmpty(serviceBean.getCluster()) ? serviceBean.getCluster() : Constants.DEFAULT_CLUSTER)
                .url("")
                .build();
        return GsonUtils.getInstance().toJson(builder);
    }

    private URIRegisterDTO buildURIRegisterDTO(@NonNull final ServiceBean serviceBean) {
        return URIRegisterDTO.builder()
                .contextPath(getContextPath())
                .appName(buildAppName(serviceBean))
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .host(buildHost())
                .port(buildPort(serviceBean))
                .build();
    }

    private String buildAppName(@NonNull final ServiceBean serviceBean) {
        return StringUtils.isBlank(getAppName()) ? serviceBean.getApplication().getName() : getAppName();
    }

    private String buildHost() {
        return IpUtils.isCompleteHost(getHost()) ? getHost() : IpUtils.getHost(getHost());
    }

    private int buildPort(@NonNull final ServiceBean serviceBean) {
        return StringUtils.isBlank(getPort()) ? serviceBean.getProtocol().getPort() : Integer.parseInt(getPort());
    }
}


