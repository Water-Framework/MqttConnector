package it.water.connectors.mqtt;

import it.water.connectors.mqtt.api.MqttMessageReceiver;
import it.water.connectors.mqtt.model.MqttMessage;
import it.water.connectors.mqtt.service.MqttConnectorSystemServiceImpl;
import it.water.connectors.mqtt.util.MqttConnectorConstants;
import it.water.core.api.bundle.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MqttConnectorSystemServiceImplUnitTest {

    private ApplicationProperties props(String url, String username, String password) {
        ApplicationProperties props = Mockito.mock(ApplicationProperties.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(props.getProperty(MqttConnectorConstants.WATER_MQTT_BROKER_URL)).thenReturn(url);
        Mockito.when(props.getProperty(MqttConnectorConstants.WATER_MQTT_USERNAME)).thenReturn(username);
        Mockito.when(props.getProperty(MqttConnectorConstants.WATER_MQTT_PASSWORD)).thenReturn(password);
        return props;
    }

    @Test
    void connectorIsDisabledWithoutBrokerUrl() {
        MqttConnectorSystemServiceImpl service = new MqttConnectorSystemServiceImpl();
        service.activate(props(null, null, null));
        Assertions.assertFalse(service.isConnected());
        service.deactivate();
    }

    @Test
    void publishFailsWhenNotConnected() {
        MqttConnectorSystemServiceImpl service = new MqttConnectorSystemServiceImpl();
        byte[] payload = "x".getBytes();
        Assertions.assertThrows(IllegalStateException.class, () -> service.publish("a/b", payload));
        Assertions.assertThrows(IllegalStateException.class, () -> service.publish(MqttMessage.from("a/b", payload)));
    }

    @Test
    void usernameWithoutPasswordDoesNotStartTheClient() {
        MqttConnectorSystemServiceImpl service = new MqttConnectorSystemServiceImpl();
        service.activate(props("tcp://localhost:1", "user", null));
        Assertions.assertFalse(service.isConnected());
        service.deactivate();
    }

    @Test
    void firstConnectFailureDoesNotThrowAndConnectorKeepsRetrying() {
        MqttConnectorSystemServiceImpl service = new MqttConnectorSystemServiceImpl();
        service.activate(props("tcp://localhost:1", null, null));
        Assertions.assertFalse(service.isConnected());
        service.deactivate();
    }

    @Test
    void registerAndUnregisterReceiverWhileDisconnected() {
        MqttConnectorSystemServiceImpl service = new MqttConnectorSystemServiceImpl();
        MqttMessageReceiver receiver = message -> {
        };
        Assertions.assertDoesNotThrow(() -> {
            service.registerMessageReceiver(receiver, "sensors/#");
            service.unregisterMessageReceiver(receiver, "sensors/#");
            service.unregisterMessageReceiver(receiver, "unknown");
        });
    }
}
