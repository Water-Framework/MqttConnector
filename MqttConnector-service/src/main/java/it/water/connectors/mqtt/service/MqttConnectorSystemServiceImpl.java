package it.water.connectors.mqtt.service;

import it.water.connectors.mqtt.api.MqttConnectorSystemApi;
import it.water.connectors.mqtt.api.MqttMessageReceiver;
import it.water.connectors.mqtt.model.MqttMessage;
import it.water.connectors.mqtt.util.MqttConnectorConstants;
import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.service.BaseSystemServiceImpl;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT connector based on Eclipse Paho.
 * <p>
 * The first connection is retried in background until it succeeds, subscriptions are re-established on
 * every (re)connection and received messages are dispatched to the registered receivers on a worker pool.
 */
@FrameworkComponent
public class MqttConnectorSystemServiceImpl extends BaseSystemServiceImpl implements MqttConnectorSystemApi {

    private static final long DEFAULT_RECONNECT_DELAY_MS = 5000;

    private final Map<String, CopyOnWriteArrayList<MqttMessageReceiver>> messageReceivers = new ConcurrentHashMap<>();

    private volatile MqttClient mqttClient;
    private MqttConnectOptions connectOptions;
    private ScheduledExecutorService connectExecutor;
    private ExecutorService dispatchExecutor;
    private int defaultQos = 1;
    private long reconnectDelayMs = DEFAULT_RECONNECT_DELAY_MS;
    private volatile boolean active;

    @OnActivate
    public void activate(ApplicationProperties applicationProperties) {
        String url = applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_BROKER_URL, "");
        if (url.isBlank()) {
            getLog().info("MQTT Connector disabled: {} not set", MqttConnectorConstants.WATER_MQTT_BROKER_URL);
            return;
        }
        try {
            // The broker allows only one connection per client id: when a second one connects with the same id,
            // the broker drops the first, which auto-reconnects and drops the other, in an endless loop.
            // So every app instance must get a distinct client id: set it.water.node.id (or the client id) per instance.
            String nodeId = applicationProperties.getPropertyOrDefault("it.water.node.id", "default");

            // unique value to connect to the broker
            String clientId = applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_CLIENT_ID, "");
            if (clientId.isBlank())
                clientId = "water-mqtt-" + nodeId;
            // QoS (delivery guarantee) used for subscribe and default publish:
            // 0 = at most once (may be lost), 1 = at least once (may be duplicated), 2 = exactly once
            this.defaultQos = (int) applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_DEFAULT_QOS, 1L);
           // how much you wait until retrying the first connect
            this.reconnectDelayMs = applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_RECONNECT_DELAY_MS, DEFAULT_RECONNECT_DELAY_MS);
            int dispatchThreads = (int) applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_DISPATCH_THREADS, 1L);

            this.connectOptions = buildConnectOptions(applicationProperties);
            this.mqttClient = new MqttClient(url, clientId, new MemoryPersistence());
            this.mqttClient.setCallback(new ReconnectingCallback());
            // a single thread keeps message ordering; raise it only if receivers are order-independent
            this.dispatchExecutor = Executors.newFixedThreadPool(Math.max(1, dispatchThreads));
            this.connectExecutor = Executors.newSingleThreadScheduledExecutor();
            this.active = true;
            getLog().info("MQTT Connector activating, broker {} clientId {}", url, clientId);
            this.connectExecutor.execute(this::connectWithRetry);
        } catch (Exception e) {
            getLog().error("MQTT Connector activation failed: {}", e.getMessage(), e);
        }
    }

    @OnDeactivate
    public void deactivate() {
        this.active = false;
        if (this.connectExecutor != null)
            this.connectExecutor.shutdownNow();
        if (this.dispatchExecutor != null)
            this.dispatchExecutor.shutdown();
        try {
            if (mqttClient != null) {
                if (mqttClient.isConnected())
                    mqttClient.disconnect();
                mqttClient.close();
                getLog().info("MQTT disconnected");
            }
        } catch (MqttException e) {
            getLog().warn("Error during MQTT disconnect", e);
        }
    }

    private MqttConnectOptions buildConnectOptions(ApplicationProperties applicationProperties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_CLEAN_SESSION, true));
        options.setAutomaticReconnect(true);
        String username = applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_USERNAME, "");
        String password = applicationProperties.getPropertyOrDefault(MqttConnectorConstants.WATER_MQTT_PASSWORD, "");
        if (!username.isBlank()) {
            if (password.isBlank())
                throw new IllegalArgumentException(MqttConnectorConstants.WATER_MQTT_USERNAME + " is set but "
                    + MqttConnectorConstants.WATER_MQTT_PASSWORD + " is missing");
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        return options;
    }

    /**
     * Paho's automatic reconnect does not apply to the very first connect, so it is retried here.
     */
    private void connectWithRetry() {
        if (!active || mqttClient.isConnected())
            return;
        try {
            // subscriptions are done in connectComplete
            mqttClient.connect(connectOptions);
        } catch (MqttException e) {
            getLog().warn("MQTT connect failed ({}), retrying in {} ms", e.getMessage(), reconnectDelayMs);
            if (active)
                connectExecutor.schedule(this::connectWithRetry, reconnectDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void subscribeAll() {
        messageReceivers.keySet().forEach(this::subscribe);
    }

    private void subscribe(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, defaultQos);
                getLog().info("MQTT subscribed to topic: {}", topic);
            }
        } catch (MqttException e) {
            getLog().error("MQTT subscribe to {} failed: {}", topic, e.getMessage(), e);
        }
    }

    private void dispatch(String receivedTopic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
        MqttMessage msg = MqttMessage.from(receivedTopic, message.getPayload(), message.getQos(), message.isRetained());
        messageReceivers.forEach((filter, receivers) -> {
            if (MqttTopic.isMatched(filter, receivedTopic))
                receivers.forEach(receiver -> dispatchExecutor.execute(() -> {
                    try {
                        receiver.receive(msg);
                    } catch (Exception e) {
                        getLog().error("MQTT receiver {} failed on topic {}: {}", receiver.getClass().getName(), receivedTopic, e.getMessage(), e);
                    }
                }));
        });
    }


    @Override
    public void registerMessageReceiver(MqttMessageReceiver receiver, String topic) {

        //Thread safe
        CopyOnWriteArrayList<MqttMessageReceiver> receivers =
                messageReceivers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>());
        boolean firstOnTopic = receivers.isEmpty();
        // addIfAbsent: registering the same receiver twice on a topic must not duplicate it (atomic check+add)
        receivers.addIfAbsent(receiver);
        if (firstOnTopic)
            subscribe(topic);
    }

    @Override
    public void unregisterMessageReceiver(MqttMessageReceiver receiver, String topic) {
        List<MqttMessageReceiver> receivers = messageReceivers.get(topic);
        if (receivers == null)
            return;
        receivers.remove(receiver);
        if (receivers.isEmpty() && messageReceivers.remove(topic, receivers)) {
            try {
                if (mqttClient != null && mqttClient.isConnected())
                    mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                getLog().warn("MQTT unsubscribe from {} failed: {}", topic, e.getMessage());
            }
        }
    }

    @Override
    public void publish(MqttMessage message) {
        if (!isConnected())
            throw new IllegalStateException("MQTT connector is not connected to the broker");
        try {
            mqttClient.publish(message.getTopic(), message.getPayload(), message.getQos(), message.isRetained());
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT publish on " + message.getTopic() + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String topic, byte[] payload) {
        publish(MqttMessage.from(topic, payload, defaultQos, false));
    }

    @Override
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    private class ReconnectingCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            getLog().info("MQTT connected to {} (reconnect={})", serverURI, reconnect);
            // with cleanSession=true the broker forgets subscriptions, so they must be redone on every connect
            subscribeAll();
        }

        @Override
        public void connectionLost(Throwable cause) {
            getLog().warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
        }

        // Paho populate topic and message when someone publish on topic that the client is subscribed
        @Override
        public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
            dispatch(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // nothing to do: publish is synchronous
        }
    }
}
