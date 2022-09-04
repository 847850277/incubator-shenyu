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
import org.apache.shenyu.client.dubbo.agent.bean.SpringCloudClientEventListener;

/**
 * MethodInterceptor.
 */
public class MethodTransferInterceptor {

    /**
     * MethodInterceptor.
     *
     * @param args args.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC)Object[] args) {
//        SpringCloudClientEventListener springCloudClientEventListener = new SpringCloudClientEventListener();
//        if (args.length == 0) {
//            args = new Object[]{springCloudClientEventListener};
//        } else {
//            Object[] newArgs = new Object[args.length + 1];
//            newArgs[args.length] = springCloudClientEventListener;
//            args = newArgs;
//        }
        SpringCloudClientEventListener springCloudClientEventListener = new SpringCloudClientEventListener();
        Object[] newArgs = new Object[args.length + 1];
        //newArgs[0] = springCloudClientEventListener;
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = springCloudClientEventListener;
        args = newArgs;
    }

}
