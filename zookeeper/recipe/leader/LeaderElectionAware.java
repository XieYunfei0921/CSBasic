/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.recipes.leader;

import org.apache.zookeeper.recipes.leader.LeaderElectionSupport.EventType;

/**
 * Leader选举标识
 *
 * 这个接口通过客户端实现，可以接受选举事件
 */
public interface LeaderElectionAware {

    /**
     *每次状态转换的时候调用.当前,低级事件在状态的起始和结束提供.例如START可以跟随在OFFER_START,OFFER_COMPLETE,DETERMINE_START
     * ,DETERMINE_COMPLETE 之后.
     *
     * @param eventType 事件类型
     */
    void onElectionEvent(EventType eventType);

}
