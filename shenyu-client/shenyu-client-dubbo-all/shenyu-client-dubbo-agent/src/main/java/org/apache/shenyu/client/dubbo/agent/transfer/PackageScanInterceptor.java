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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MethodInterceptor.
 */
public class PackageScanInterceptor {

    /**
     * MethodInterceptor.
     *
     * @param args args.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) throws Throwable {
        for (Object arg : args) {
            if (arg instanceof LinkedHashMap) {
                Map<String, Object> maps = (Map<String, Object>) arg;
                String[] baseScan = (String[]) maps.get("basePackages");
                if (baseScan.length > 0) {
                    String[] newBaseScan = new String[baseScan.length + 1];
                    System.arraycopy(baseScan, 0, newBaseScan, 0, baseScan.length);
                    newBaseScan[baseScan.length] = "org.apache.shenyu.client.dubbo.agent.bean";
                    maps.put("basePackages", newBaseScan);
                } else {
                    final int length = args.length;
                    if (args[length - 1] instanceof String) {
                        String declaringClass = String.valueOf(args[length - 1]);
                        String originalPackageScan = declaringClass.substring(0, declaringClass.lastIndexOf("."));
                        String[] packageScans = new String[]{originalPackageScan, "org.apache.shenyu.client.dubbo.agent.bean"};
                        maps.put("basePackages", packageScans);
                    }
                }
            }
        }
    }

}
