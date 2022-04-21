/*
 * (c) Copyright 2022 James Baker. All rights reserved.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;    http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.jbaker.automation.integrations.trains;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;

public final class TrainRoute {
    private static final boolean NO_JAVASCRIPT = false;
    private static final String NO_PROXY_HOST = null;
    private static final int NO_PROXY_PORT = -1;
    private static final String URL_TEMPLATE = "https://ojp.nationalrail.co.uk/service/ldbboard/dep/%s/%s/To";
    private static final String BAD_STATUS_PATTERN = "(Cancelled|Delayed)";
    private static final int ROUTE_POSSIBLY_DISRUPTED = 2;

    private final String fromToc;
    private final String toToc;
    private final String url;

    public TrainRoute(String fromToc, String toToc) {
        this.fromToc = fromToc;
        this.toToc = toToc;
        this.url = URL_TEMPLATE.formatted(fromToc, toToc);
    }

    public boolean isServiceDisrupted() {
        try (WebClient browser = new WebClient(BrowserVersion.CHROME, NO_JAVASCRIPT, NO_PROXY_HOST, NO_PROXY_PORT)) {
            HtmlPage page = browser.getPage(url);
            HtmlDivision table = page.getFirstByXPath("//div[contains(@class, 'results trains')]");
            String text = table.asNormalizedText();
            int badTrains = text.split(BAD_STATUS_PATTERN).length - 1;
            return badTrains >= ROUTE_POSSIBLY_DISRUPTED;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
