/*
 * Copyright 2016-2017 Direktoratet for forvaltning og IKT
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.vefa.peppol.lookup.fetcher;

import no.difi.vefa.peppol.lookup.api.FetcherResponse;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.mode.Mode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedInputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

public class ApacheFetcher extends AbstractFetcher {

    private HttpClient httpClient;

    private RequestConfig requestConfig;

    public ApacheFetcher(Mode mode, HttpClient httpClient) {
        super(mode);
        this.httpClient = httpClient;

        this.requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
    }

    public ApacheFetcher(Mode mode) {
        this(mode, HttpClients.createDefault());
    }

    @Override
    public FetcherResponse fetch(URI uri) throws LookupException {
        try {
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(requestConfig);

            HttpResponse response = httpClient.execute(httpGet);

            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    return new FetcherResponse(
                            new BufferedInputStream(response.getEntity().getContent()),
                            response.containsHeader("X-SMP-Namespace") ?
                                    response.getFirstHeader("X-SMP-Namespace").getValue() : null
                    );
                case 404:
                    throw new LookupException("Not supported.");
                default:
                    throw new LookupException(
                            String.format("Received code %s for lookup.", response.getStatusLine().getStatusCode()));
            }
        } catch (SocketTimeoutException | SocketException | UnknownHostException e) {
            throw new LookupException(String.format("Unable to fetch '%s'", uri), e);
        } catch (LookupException e) {
            throw e;
        } catch (Exception e) {
            throw new LookupException(e.getMessage(), e);
        }
    }
}
