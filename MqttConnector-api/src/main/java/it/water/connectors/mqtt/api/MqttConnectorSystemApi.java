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
import it.water.core.api.service.BaseSystemApi;

public interface MqttConnectorSystemApi extends BaseSystemApi {

    /**
     * Registers a receiver for a topic filter (MQTT wildcards {@code +} and {@code #} are supported).
     * The subscription is (re)established automatically on every (re)connection to the broker.
     *
     * @param receiver receiver to register
     * @param topic    topic or topic filter
     */
    void registerMessageReceiver(MqttMessageReceiver receiver, String topic);

    /**
     * Unregisters a receiver. The broker subscription is dropped when no receiver is left on the topic.
     *
     * @param receiver receiver to unregister
     * @param topic    topic or topic filter
     */
    void unregisterMessageReceiver(MqttMessageReceiver receiver, String topic);

    /**
     * Publishes a message.
     *
     * @param message message to publish
     * @throws IllegalStateException if the connector is not connected to the broker
     */
    void publish(MqttMessage message);

    /**
     * Publishes a payload on a topic using the default qos and no retain.
     *
     * @param topic   destination topic
     * @param payload message payload
     * @throws IllegalStateException if the connector is not connected to the broker
     */
    void publish(String topic, byte[] payload);

    /**
     * @return true if the connector is currently connected to the broker
     */
    boolean isConnected();
}
