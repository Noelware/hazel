/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
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

package org.noelware.hazel.modules.logging.json;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.noelware.hazel.HazelInfo;

public class ClassicJsonLayout extends AbstractJsonLayout<ILoggingEvent> {
    private final ThrowableHandlingConverter throwableProxyConverter = new ThrowableProxyConverter();

    @Override
    Map<String, Object> toJsonMap(ILoggingEvent event) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final DateFormat formatter = new SimpleDateFormat(getTimestampFormat());
        formatter.setTimeZone(TimeZone.getTimeZone(getTimezone()));

        data.put("@timestamp", formatter.format(new Date(event.getTimeStamp())));
        data.put("message", event.getFormattedMessage());
        data.put("thread", event.getThreadName());
        data.put("log.context", event.getLoggerContextVO().getName());
        data.put("log.level", event.getLevel().levelStr);
        data.put("log.name", event.getLoggerName());

        // === metadata ===
        data.put("hazel.distribution", HazelInfo.getDistribution().name().toLowerCase(Locale.ROOT));
        data.put("hazel.build.date", HazelInfo.getBuildDate());
        data.put("hazel.git.commit", HazelInfo.getCommitHash());
        data.put("hazel.version", HazelInfo.getVersion());
        data.put("metadata.product", "Hazel");
        data.put("metadata.vendor", "Noelware");

        if (HazelInfo.getDedicatedNode() != null) {
            data.put("metadata.dedi.node", HazelInfo.getDedicatedNode());
        }

        final Map<String, String> mdc = event.getMDCPropertyMap();
        data.putAll(mdc);

        final var throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            final var exception = throwableProxyConverter.convert(event);
            if (exception != null && !exception.isEmpty()) {
                data.put("exception", exception);
            }
        }

        return data;
    }

    @Override
    public void start() {
        throwableProxyConverter.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        throwableProxyConverter.stop();
    }
}
