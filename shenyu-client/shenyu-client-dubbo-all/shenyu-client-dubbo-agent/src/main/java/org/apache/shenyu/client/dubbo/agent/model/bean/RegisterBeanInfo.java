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

package org.apache.shenyu.client.dubbo.agent.model.bean;

/**
 * RegisterBeanInfo.
 */
public class RegisterBeanInfo {

    private static String registerType;

    private static String serverLists;

    private static String username;

    private static String password;

    private static String appName;

    private static String contextPath;

    private static String port;

    private static String host;

    /**
     * getPort.
     *
     * @return port.
     */
    public static String getPort() {
        return port;
    }

    /**
     * setPort.
     *
     * @param port port to set.
     */
    public static void setPort(final String port) {
        RegisterBeanInfo.port = port;
    }

    /**
     * getHost.
     *
     * @return host.
     */
    public static String getHost() {
        return host;
    }

    /**
     * setHost.
     *
     * @param host host to set.
     */
    public static void setHost(final String host) {
        RegisterBeanInfo.host = host;
    }

    /**
     * getRegisterType.
     *
     * @return registerType.
     */
    public static String getRegisterType() {
        return registerType;
    }

    /**
     * setRegisterType.
     *
     * @param registerType registerType to set.
     */
    public static void setRegisterType(final String registerType) {
        RegisterBeanInfo.registerType = registerType;
    }

    /**
     * getServerLists.
     *
     * @return server list.
     */
    public static String getServerLists() {
        return serverLists;
    }

    /**
     * setServerLists.
     *
     * @param serverLists server list.
     */
    public static void setServerLists(final String serverLists) {
        RegisterBeanInfo.serverLists = serverLists;
    }

    /**
     * getusername.
     *
     * @return username.
     */
    public static String getUsername() {
        return username;
    }

    /**
     * setusername.
     *
     * @param username username to set.
     */
    public static void setUsername(final String username) {
        RegisterBeanInfo.username = username;
    }

    /**
     * getpassword.
     *
     * @return password.
     */
    public static String getPassword() {
        return password;
    }

    /**
     * ser password.
     *
     * @param password password to set.
     */
    public static void setPassword(final String password) {
        RegisterBeanInfo.password = password;
    }

    /**
     * get appName.
     *
     * @return appName.
     */
    public static String getAppName() {
        return appName;
    }

    /**
     * set appName.
     *
     * @param appName app name to set.
     */
    public static void setAppName(final String appName) {
        RegisterBeanInfo.appName = appName;
    }

    /**
     * get contextPath.
     *
     * @return contextPath.
     */
    public static String getContextPath() {
        return contextPath;
    }

    /**
     * set contextPath.
     *
     * @param contextPath context path to set.
     */
    public static void setContextPath(final String contextPath) {
        RegisterBeanInfo.contextPath = contextPath;
    }
}
