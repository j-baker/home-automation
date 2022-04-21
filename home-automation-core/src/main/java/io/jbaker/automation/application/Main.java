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

package io.jbaker.automation.application;

import com.codahale.metrics.health.HealthCheck.Result;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.jbaker.automation.application.config.AutomationConfiguration;
import io.jbaker.automation.application.config.MqttConfig;
import io.jbaker.automation.application.config.RouteConfig;
import io.jbaker.automation.comms.BinarySensor;
import io.jbaker.automation.comms.Messager;
import io.jbaker.automation.integrations.trains.TrainRoute;
import io.jbaker.automation.task.ScraperTask;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main extends Application<AutomationConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final Duration TRAIN_PING_PERIOD = Duration.ofMinutes(15);
    private static final String MQTT_CLIENT_ID = "home-automation-app";

    private Main() {}

    public static void main(String[] _args) throws Exception {
        new Main().run("server", "var/conf/install.yml");
    }

    @Override
    public void initialize(Bootstrap<AutomationConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(AutomationConfiguration configuration, Environment environment) throws Exception {
        MqttClient client = createMqttClient(configuration.getMqtt(), environment);
        Messager messager = new Messager(client);
        setupTrainDisruptionTasks(environment, messager, configuration.getRoutes());
        setupHealthcheckVariable(messager, environment);
    }

    private void setupTrainDisruptionTasks(Environment environment, Messager messager, List<RouteConfig> routes) {
        ScheduledExecutorService executor = environment
                .lifecycle()
                .scheduledExecutorService("train-disruption-%d")
                .threads(1)
                .build();
        for (RouteConfig routeConfig : routes) {
            String name = "train-route-" + routeConfig.getName();
            TrainRoute route = new TrainRoute(routeConfig.getFrom(), routeConfig.getTo());
            Consumer<Boolean> sender = messager.register(new BinarySensor(name, "motion"));
            ScraperTask scraperTask = ScraperTask.createAndStart(
                    executor, TRAIN_PING_PERIOD, () -> sender.accept(route.isServiceDisrupted()));
            environment.healthChecks().register(name, scraperTask);
        }
    }

    private void setupHealthcheckVariable(Messager messager, Environment environment) {
        ScheduledExecutorService executor = environment
                .lifecycle()
                .scheduledExecutorService("healthcheck-%d")
                .threads(1)
                .build();
        Consumer<Boolean> sender = messager.register(new BinarySensor("automation-health-check", "none"));
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        sender.accept(environment.healthChecks().runHealthChecks().values().stream()
                                .allMatch(Result::isHealthy));
                    } catch (Throwable t) {
                        log.error("could not update healthcheck variable. guess we're done", t);
                    }
                },
                1,
                1,
                TimeUnit.MINUTES);
    }

    private MqttClient createMqttClient(MqttConfig config, Environment environment) throws MqttException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        String serverUri = "ssl://%s:%d".formatted(config.getHost(), config.getPort());
        connectOptions.setUserName(config.getUsername());
        connectOptions.setPassword(config.getPassword().toCharArray());
        connectOptions.setServerURIs(new String[] {serverUri});
        MqttClient client = new MqttClient(serverUri, MQTT_CLIENT_ID);

        // Immediately connect so that registrations may occur.
        client.connect(connectOptions);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() {}

            @Override
            public void stop() throws MqttException {
                client.close();
            }
        });
        return client;
    }
}
