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

package org.apache.shenyu.client.springmvc.init;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.apidocs.annotations.ApiDoc;
import org.apache.shenyu.client.core.client.AbstractContextRefreshedEventListener;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.utils.PortUtils;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.apache.shenyu.common.enums.ApiHttpMethodEnum;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.common.utils.PathUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.ApiDocRegisterDTO;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Shenyu spring mvc client event listener.
 */
public class SpringMvcClientEventListener extends AbstractContextRefreshedEventListener<Object, ShenyuSpringMvcClient> {

    private final List<Class<? extends Annotation>> mappingAnnotation = new ArrayList<>(3);

    private final Boolean isFull;

    private final String protocol;

    private final boolean addPrefixed;

    /**
     * Instantiates a new context refreshed event listener.
     *
     * @param clientConfig                   the shenyu client config
     * @param shenyuClientRegisterRepository the shenyuClientRegisterRepository
     */
    public SpringMvcClientEventListener(final PropertiesConfig clientConfig,
                                        final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        Properties props = clientConfig.getProps();
        this.isFull = Boolean.parseBoolean(props.getProperty(ShenyuClientConstants.IS_FULL, Boolean.FALSE.toString()));
        this.protocol = props.getProperty(ShenyuClientConstants.PROTOCOL, ShenyuClientConstants.HTTP);
        this.addPrefixed = Boolean.parseBoolean(props.getProperty(ShenyuClientConstants.ADD_PREFIXED,
                Boolean.FALSE.toString()));
        mappingAnnotation.add(ShenyuSpringMvcClient.class);
        mappingAnnotation.add(RequestMapping.class);
    }

    @Override
    protected Map<String, Object> getBeans(final ApplicationContext context) {
        // Filter out
        if (Boolean.TRUE.equals(isFull)) {
            getPublisher().publishEvent(MetaDataRegisterDTO.builder()
                    .contextPath(getContextPath())
                    .addPrefixed(addPrefixed)
                    .appName(getAppName())
                    .path(PathUtils.decoratorPathWithSlash(getContextPath()))
                    .rpcType(RpcTypeEnum.HTTP.getName())
                    .enabled(true)
                    .ruleName(getContextPath())
                    .build());
            return null;
        }
        return context.getBeansWithAnnotation(Controller.class);
    }

    @Override
    protected URIRegisterDTO buildURIRegisterDTO(final ApplicationContext context,
                                                 final Map<String, Object> beans) {
        try {
            final String host = getHost();
            final int port = Integer.parseInt(Optional.ofNullable(getPort()).orElseGet(() -> "-1"));
            final int mergedPort = port <= 0 ? PortUtils.findPort(context.getAutowireCapableBeanFactory()) : port;
            return URIRegisterDTO.builder()
                    .contextPath(getContextPath())
                    .appName(getAppName())
                    .protocol(protocol)
                    .host(IpUtils.isCompleteHost(host) ? host : IpUtils.getHost(host))
                    .port(mergedPort)
                    .rpcType(RpcTypeEnum.HTTP.getName())
                    .build();
        } catch (ShenyuException e) {
            throw new ShenyuException(e.getMessage() + "please config ${shenyu.client.http.props.port} in xml/yml !");
        }
    }

    @Override
    protected String buildApiSuperPath(final Class<?> clazz, @Nullable final ShenyuSpringMvcClient beanShenyuClient) {
        if (Objects.nonNull(beanShenyuClient) && StringUtils.isNotBlank(beanShenyuClient.path())) {
            return beanShenyuClient.path();
        }
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
        // Only the first path is supported temporarily
        if (Objects.nonNull(requestMapping) && ArrayUtils.isNotEmpty(requestMapping.path()) && StringUtils.isNotBlank(requestMapping.path()[0])) {
            return requestMapping.path()[0];
        }
        return "";
    }

    @Override
    protected Class<ShenyuSpringMvcClient> getAnnotationType() {
        return ShenyuSpringMvcClient.class;
    }

    @Override
    protected void handleMethod(final Object bean, final Class<?> clazz,
                                @Nullable final ShenyuSpringMvcClient beanShenyuClient,
                                final Method method, final String superPath) {
        final RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        ShenyuSpringMvcClient methodShenyuClient = AnnotatedElementUtils.findMergedAnnotation(method, ShenyuSpringMvcClient.class);
        methodShenyuClient = Objects.isNull(methodShenyuClient) ? beanShenyuClient : methodShenyuClient;
        // the result of ReflectionUtils#getUniqueDeclaredMethods contains method such as hashCode, wait, toSting
        // add Objects.nonNull(requestMapping) to make sure not register wrong method
        if (Objects.nonNull(methodShenyuClient) && Objects.nonNull(requestMapping)) {
            getPublisher().publishEvent(buildMetaDataDTO(bean, methodShenyuClient, buildApiPath(method, superPath, methodShenyuClient), clazz, method));
        }
    }

    @Override
    protected String buildApiPath(final Method method, final String superPath,
                                  @NonNull final ShenyuSpringMvcClient methodShenyuClient) {
        String contextPath = getContextPath();
        if (StringUtils.isNotBlank(methodShenyuClient.path())) {
            return pathJoin(contextPath, superPath, methodShenyuClient.path());
        }
        final String path = getPathByMethod(method);
        if (StringUtils.isNotBlank(path)) {
            return pathJoin(contextPath, superPath, path);
        }
        return pathJoin(contextPath, superPath);
    }

    /**
     * buildApiDocDTO.
     *
     * @param clazz  clazz
     * @param method method
     * @return ApiDocRegisterDTO
     */
    @Override
    protected List<ApiDocRegisterDTO> buildApiDocDTO(final Class<?> clazz, final Method method) {
        final String contextPath = getContextPath();
        Map<RequestMethod, ApiDocRegisterDTO> methodPairMap = buildMethodPairMapByDeclaredAnnotations(clazz, method, contextPath);
        //获取httpMethod 、consume、produce
        List<ApiDocRegisterDTO> list = Lists.newArrayList();
        methodPairMap.forEach((k, v) -> {
            list.add(v);
        });
        return list;
    }

    private Map<RequestMethod, ApiDocRegisterDTO> buildMethodPairMapByDeclaredAnnotations(final Class<?> clazz, final Method method, final String contextPath) {
        Map<RequestMethod, ApiDocRegisterDTO> map = Maps.newHashMap();
        Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
        String apiDesc = Stream.of(declaredAnnotations).filter(item -> item instanceof ApiDoc).findAny().map(item -> {
            ApiDoc apiDoc = (ApiDoc) item;
            return apiDoc.desc();
        }).orElse("");
        String superPath = buildApiSuperPath(clazz, AnnotatedElementUtils.findMergedAnnotation(clazz, getAnnotationType()));
        if (superPath.indexOf("*") > 0) {
            superPath = superPath.substring(0, superPath.lastIndexOf("/"));
        }
        for (Annotation declaredAnnotation : declaredAnnotations) {
            if (declaredAnnotation instanceof RequestMapping) {
                RequestMapping requestMapping = (RequestMapping) declaredAnnotation;
                String produce = requestMapping.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", requestMapping.produces());
                String consume = requestMapping.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", requestMapping.consumes());
                String[] values = requestMapping.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    for (RequestMethod requestMethod : requestMapping.method()) {
                        ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                                .consume(consume)
                                .produce(produce)
                                .httpMethod(ApiHttpMethodEnum.getValueByName(String.valueOf(requestMethod)))
                                .contextPath(contextPath)
                                .ext("{}")
                                .document("{}")
                                .version("v0.01")
                                .rpcType(RpcTypeEnum.HTTP.name())
                                .apiDesc(apiDesc)
                                .apiPath(apiPath)
                                .apiSource(1)
                                .state(1)
                                .apiOwner("1")
                                .build();
                        map.put(requestMethod, build);
                    }
                }
                return map;
            }
            if (declaredAnnotation instanceof PostMapping) {
                PostMapping post = (PostMapping) declaredAnnotation;
                String produce = post.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", post.produces());
                String consume = post.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", post.consumes());
                String[] values = post.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                            .consume(consume)
                            .produce(produce)
                            .httpMethod(ApiHttpMethodEnum.POST.getValue())
                            .contextPath(contextPath)
                            .ext("{}")
                            .document("{}")
                            .version("v0.01")
                            .rpcType(RpcTypeEnum.HTTP.name())
                            .apiDesc(apiDesc)
                            .apiPath(apiPath)
                            .apiSource(1)
                            .state(1)
                            .apiOwner("1")
                            .build();
                    map.put(RequestMethod.POST, build);
                }
            }
            if (declaredAnnotation instanceof GetMapping) {
                GetMapping get = (GetMapping) declaredAnnotation;
                String produce = get.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", get.produces());
                String consume = get.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", get.consumes());
                String[] values = get.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                            .consume(consume)
                            .produce(produce)
                            .httpMethod(ApiHttpMethodEnum.GET.getValue())
                            .contextPath(contextPath)
                            .ext("{}")
                            .document("{}")
                            .version("v0.01")
                            .rpcType(RpcTypeEnum.HTTP.name())
                            .apiDesc(apiDesc)
                            .apiPath(apiPath)
                            .apiSource(1)
                            .state(1)
                            .apiOwner("1")
                            .build();
                    map.put(RequestMethod.GET, build);
                }
            }
            if (declaredAnnotation instanceof PutMapping) {
                PutMapping put = (PutMapping) declaredAnnotation;
                String produce = put.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", put.produces());
                String consume = put.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", put.consumes());
                String[] values = put.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                            .consume(consume)
                            .produce(produce)
                            .httpMethod(ApiHttpMethodEnum.PUT.getValue())
                            .contextPath(contextPath)
                            .ext("{}")
                            .document("{}")
                            .version("v0.01")
                            .rpcType(RpcTypeEnum.HTTP.name())
                            .apiDesc(apiDesc)
                            .apiPath(apiPath)
                            .apiSource(1)
                            .state(1)
                            .apiOwner("1")
                            .build();
                    map.put(RequestMethod.PUT, build);
                }
            }
            if (declaredAnnotation instanceof DeleteMapping) {
                DeleteMapping delete = (DeleteMapping) declaredAnnotation;
                String produce = delete.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", delete.produces());
                String consume = delete.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", delete.consumes());
                String[] values = delete.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                            .consume(consume)
                            .produce(produce)
                            .httpMethod(ApiHttpMethodEnum.DELETE.getValue())
                            .contextPath(contextPath)
                            .ext("{}")
                            .document("{}")
                            .version("v0.01")
                            .rpcType(RpcTypeEnum.HTTP.name())
                            .apiDesc(apiDesc)
                            .apiPath(apiPath)
                            .apiSource(1)
                            .state(1)
                            .apiOwner("1")
                            .build();
                    map.put(RequestMethod.DELETE, build);
                }
            }
            if (declaredAnnotation instanceof PatchMapping) {
                PatchMapping patch = (PatchMapping) declaredAnnotation;
                String produce = patch.produces().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", patch.produces());
                String consume = patch.consumes().length == 0 ? ShenyuClientConstants.MEDIA_TYPE_ALL_VALUE : String.join(",", patch.consumes());
                String[] values = patch.value();
                for (String value : values) {
                    String apiPath = contextPath + superPath + value;
                    ApiDocRegisterDTO build = ApiDocRegisterDTO.builder()
                            .consume(consume)
                            .produce(produce)
                            .httpMethod(ApiHttpMethodEnum.PATCH.getValue())
                            .contextPath(contextPath)
                            .ext("{}")
                            .document("{}")
                            .version("v0.01")
                            .rpcType(RpcTypeEnum.HTTP.name())
                            .apiDesc(apiDesc)
                            .apiPath(apiPath)
                            .apiSource(1)
                            .state(1)
                            .apiOwner("1")
                            .build();
                    map.put(RequestMethod.PATCH, build);
                }
            }
        }
        return map;
    }

    private String getPathByMethod(@NonNull final Method method) {
        for (Class<? extends Annotation> mapping : mappingAnnotation) {
            final String pathByAnnotation = getPathByAnnotation(AnnotatedElementUtils.findMergedAnnotation(method, mapping));
            if (StringUtils.isNotBlank(pathByAnnotation)) {
                return pathByAnnotation;
            }
        }
        return null;
    }

    private String getPathByAnnotation(@Nullable final Annotation annotation) {
        if (Objects.isNull(annotation)) {
            return null;
        }
        final Object value = AnnotationUtils.getValue(annotation, "value");
        if (value instanceof String && StringUtils.isNotBlank((String) value)) {
            return (String) value;
        }
        // Only the first path is supported temporarily
        if (value instanceof String[] && ArrayUtils.isNotEmpty((String[]) value) && StringUtils.isNotBlank(((String[]) value)[0])) {
            return ((String[]) value)[0];
        }
        return null;
    }

    @Override
    protected MetaDataRegisterDTO buildMetaDataDTO(final Object bean,
                                                   @NonNull final ShenyuSpringMvcClient shenyuClient,
                                                   final String path, final Class<?> clazz,
                                                   final Method method) {
        return MetaDataRegisterDTO.builder()
                .contextPath(getContextPath())
                .addPrefixed(addPrefixed)
                .appName(getAppName())
                .serviceName(clazz.getName())
                .methodName(Optional.ofNullable(method).map(Method::getName).orElse(null))
                .path(path)
                .pathDesc(shenyuClient.desc())
                .parameterTypes(Optional.ofNullable(method)
                        .map(m -> Arrays.stream(m.getParameterTypes())
                                .map(Class::getName)
                                .collect(Collectors.joining(","))
                        ).orElse(null))
                .rpcType(RpcTypeEnum.HTTP.getName())
                .enabled(shenyuClient.enabled())
                .ruleName(StringUtils.defaultIfBlank(shenyuClient.ruleName(), path))
                .registerMetaData(shenyuClient.registerMetaData())
                .build();
    }
}
