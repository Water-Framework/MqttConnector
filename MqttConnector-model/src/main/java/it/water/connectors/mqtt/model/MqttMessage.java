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
package it.water.connectors.mqtt.model;

import lombok.Getter;

/**
 * Broker-agnostic representation of an MQTT message, used both for received and published messages.
 */
@Getter
public class MqttMessage {
    private final String topic;
    private final byte[] payload;
    private final int qos;
    private final boolean retained;

    public MqttMessage(String topic, byte[] payload, int qos, boolean retained) {
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retained = retained;
    }

    public static MqttMessage from(String topic, byte[] payload) {
        return new MqttMessage(topic, payload, 1, false);
    }

    public static MqttMessage from(String topic, byte[] payload, int qos, boolean retained) {
        return new MqttMessage(topic, payload, qos, retained);
    }

    @Override
    public String toString() {
        return "MQTT MESSAGE ON TOPIC:" + topic + " QOS:" + qos
            + (payload != null ? " MESSAGE IS:" + new String(payload) : "");
    }
}
