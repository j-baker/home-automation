/*
 * (c) Copyright 2022 James Baker. All rights reserved.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;    http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.jbaker.automation.task;

import com.codahale.metrics.health.HealthCheck;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScraperTask extends HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(ScraperTask.class);

    private final Random random = new Random();
    private final Duration period;
    private final Runnable actualTask;
    private final ScheduledExecutorService executor;

    private volatile boolean lastRunSucceeded = true;

    private ScraperTask(Duration period, Runnable actualTask, ScheduledExecutorService executor) {
        this.period = period;
        this.actualTask = actualTask;
        this.executor = executor;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private void run() {
        try {
            actualTask.run();
            lastRunSucceeded = true;
        } catch (Throwable t) {
            log.warn("running the task failed", t);
            lastRunSucceeded = false;
        }
        long delay = (long) (period.toMillis() * 2 * random.nextDouble());
        executor.schedule(this::run, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    protected Result check() {
        return lastRunSucceeded ? Result.healthy() : Result.unhealthy("last run did not succeed");
    }

    public static ScraperTask createAndStart(ScheduledExecutorService executor, Duration period, Runnable task) {
        ScraperTask ret = new ScraperTask(period, task, executor);
        ret.run();
        return ret;
    }
}
