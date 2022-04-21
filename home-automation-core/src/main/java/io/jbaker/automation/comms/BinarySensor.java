/*
 * (c) Copyright 2022 James Baker. All rights reserved.
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

package io.jbaker.automation.comms;

public final class BinarySensor implements Sensor<Boolean> {
    private static final String CONFIGURATION_TOPIC = "homeassistant/binary_sensor/%s/config";
    private static final String STATE_TOPIC = "homeassistant/binary_sensor/%s/state";
    private static final String CONFIGURATION_TEMPLATE =
            """
            {
            "name": "%s",
            "device_class": "%s",
            "state_topic": "homeassistant/binary_sensor/%s/state",
            "unique_id": "%s"
            }
            """;
    private static final String ON = "ON";
    private static final String OFF = "OFF";

    private final String sensorName;
    private final String deviceClass;

    public BinarySensor(String sensorName, String deviceClass) {
        this.sensorName = sensorName;
        this.deviceClass = deviceClass;
    }

    @Override
    public SendableMessage getConfigurationMessage() {
        return SendableMessage.create(
                CONFIGURATION_TOPIC.formatted(sensorName),
                CONFIGURATION_TEMPLATE.formatted(sensorName, deviceClass, sensorName, sensorName));
    }

    @Override
    public SendableMessage serialize(Boolean message) {
        return SendableMessage.create(STATE_TOPIC.formatted(sensorName), (message ? ON : OFF));
    }
}
