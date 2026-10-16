package it.water.connectors.mqtt;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.Role;
import it.water.core.api.role.RoleManager;
import it.water.core.api.user.UserManager;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.repository.query.Query;
import it.water.core.api.service.Service;
import it.water.core.api.permission.PermissionManager;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.testing.utils.runtime.TestRuntimeUtils;

import it.water.core.testing.utils.junit.WaterTestExtension;

import it.water.connectors.mqtt.api.*;

import lombok.Setter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Generated with Water Generator.
 * Test class for MqttConnector Services.
 

 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MqttConnectorApiTest implements Service {
    
    @Inject
    @Setter
    private ComponentRegistry componentRegistry;
    
    @Inject
    @Setter
    private MqttConnectorApi mqttconnectorApi;

    @Inject
    @Setter
    private Runtime runtime;

    /**
     * Testing basic injection of basic component for mqttconnector entity.
     */
    @Test
    @Order(1)
    void componentsInsantiatedCorrectly() {
        this.mqttconnectorApi = this.componentRegistry.findComponent(MqttConnectorApi.class, null);
        Assertions.assertNotNull(this.mqttconnectorApi);
        Assertions.assertNotNull(this.componentRegistry.findComponent(MqttConnectorSystemApi.class, null));
    }

}
