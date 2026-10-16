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
package it.water.connectors.mqtt.api;

import it.water.connectors.mqtt.model.MqttMessage;
import it.water.core.api.service.Service;

/**
 * Callback invoked for every MQTT message received on a topic the receiver has been registered for.
 */
public interface MqttMessageReceiver extends Service {

    /**
     * Invoked on a connector worker thread (never on the MQTT client thread).
     *
     * @param message received message
     */
    void receive(MqttMessage message);
}
