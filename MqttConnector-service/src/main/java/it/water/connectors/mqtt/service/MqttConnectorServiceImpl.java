/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.connectors.mqtt.service;

import it.water.connectors.mqtt.api.*;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.*;
import it.water.core.service.BaseServiceImpl;
import lombok.*;


@FrameworkComponent
public class MqttConnectorServiceImpl extends BaseServiceImpl implements MqttConnectorApi {

    @Inject
    @Getter
    @Setter
    private MqttConnectorSystemApi systemService;

    @Inject
    @Getter
    @Setter
    private ComponentRegistry componentRegistry;

    public MqttConnectorServiceImpl() {
    }

    @Override
    public boolean isConnected() {
        return systemService.isConnected();
    }
}
