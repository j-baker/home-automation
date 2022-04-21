/*
 * (c) Copyright 2022 James Baker. All rights reserved.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;    http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.jbaker.automation.comms;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public final class Messager {
    private final MqttClient mqtt;

    public Messager(MqttClient mqtt) {
        this.mqtt = mqtt;
    }

    public <T> Consumer<T> register(Sensor<T> sensor) {
        SendableMessage config = sensor.getConfigurationMessage();
        MqttMessage configMessage = toMqttMessage(config.getBody());
        configMessage.setRetained(true);
        sendMessage(config.getTopic(), configMessage);

        return message -> {
            SendableMessage serialized = sensor.serialize(message);
            sendMessage(serialized.getTopic(), toMqttMessage(serialized.getBody()));
        };
    }

    private void sendMessage(String topic, MqttMessage message) {
        try {
            mqtt.publish(topic, message);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private static MqttMessage toMqttMessage(String message) {
        MqttMessage ret = new MqttMessage();
        ret.setPayload(message.getBytes(StandardCharsets.UTF_8));
        return ret;
    }
}
