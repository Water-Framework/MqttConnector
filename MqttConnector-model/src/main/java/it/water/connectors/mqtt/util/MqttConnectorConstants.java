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
package it.water.connectors.mqtt.util;

public class MqttConnectorConstants {
    public static final String WATER_MQTT_BROKER_URL = "it.water.connectors.mqtt.broker.url";
    public static final String WATER_MQTT_CLIENT_ID = "it.water.connectors.mqtt.client.id";
    public static final String WATER_MQTT_USERNAME = "it.water.connectors.mqtt.username";
    public static final String WATER_MQTT_PASSWORD = "it.water.connectors.mqtt.password";
    public static final String WATER_MQTT_CLEAN_SESSION = "it.water.connectors.mqtt.clean.session";
    public static final String WATER_MQTT_DEFAULT_QOS = "it.water.connectors.mqtt.default.qos";
    public static final String WATER_MQTT_RECONNECT_DELAY_MS = "it.water.connectors.mqtt.reconnect.delay.ms";
    public static final String WATER_MQTT_DISPATCH_THREADS = "it.water.connectors.mqtt.dispatch.threads";

    private MqttConnectorConstants() {
    }
}
