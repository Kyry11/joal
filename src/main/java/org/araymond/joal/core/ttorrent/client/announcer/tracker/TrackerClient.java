package org.araymond.joal.core.ttorrent.client.announcer.tracker;

import com.google.common.annotations.VisibleForTesting;
import com.turn.ttorrent.client.announce.AnnounceException;
import com.turn.ttorrent.common.protocol.TrackerMessage;
import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceResponseMessage;
import com.turn.ttorrent.common.protocol.TrackerMessage.ErrorMessage;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.araymond.joal.core.ttorrent.client.announcer.request.SuccessAnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class TrackerClient {
    private static final Logger logger = LoggerFactory.getLogger(TrackerClient.class);

    private final TrackerClientUriProvider trackerClientUriProvider;
    private final ResponseHandler<TrackerMessage> trackerResponseHandler;

    public TrackerClient(final TrackerClientUriProvider trackerClientUriProvider, final ResponseHandler<TrackerMessage> trackerResponseHandler) {
        this.trackerResponseHandler = trackerResponseHandler;
        this.trackerClientUriProvider = trackerClientUriProvider;
    }

    public SuccessAnnounceResponse announce(final String requestQuery, final Iterable<Map.Entry<String, String>> headers) throws AnnounceException {
        final URI baseUri;
        try {
            while (!this.trackerClientUriProvider.get().getScheme().startsWith("http")) {
                this.trackerClientUriProvider.deleteCurrentAndMoveToNext();
            }
            baseUri = this.trackerClientUriProvider.get();
        } catch (final NoMoreUriAvailableException e) {
            throw new AnnounceException("No more valid tracker URI", e);
        }

        final TrackerMessage responseMessage;
        try {
            responseMessage = this.makeCallAndGetResponseAsByteBuffer(baseUri, requestQuery, headers);

            if (responseMessage instanceof ErrorMessage) {
                final ErrorMessage error = (ErrorMessage) responseMessage;
                throw new AnnounceException(baseUri + ": " + error.getReason());
            }
        } catch (final AnnounceException e) {
            // If the request has failed we need to move to the next tracker.
            try {
                this.trackerClientUriProvider.moveToNext();
            } catch (final NoMoreUriAvailableException e1) {
                throw new AnnounceException("No more valid tracker for torrent.", e1);
            }
            throw new AnnounceException(e.getMessage(), e);
        }

        if (!(responseMessage instanceof AnnounceResponseMessage)) {
            throw new AnnounceException("Unexpected tracker message type " + responseMessage.getType().name() + "!");
        }

        final AnnounceResponseMessage announceResponseMessage = (AnnounceResponseMessage) responseMessage;

        final int interval = announceResponseMessage.getInterval();
        // Subtract one to seeders since we are one of them.
        final int seeders = announceResponseMessage.getComplete() == 0 ? 0 : announceResponseMessage.getComplete() - 1;
        final int leechers = announceResponseMessage.getIncomplete();
        return new SuccessAnnounceResponse(interval, seeders, leechers);
    }

    @VisibleForTesting
    TrackerMessage makeCallAndGetResponseAsByteBuffer(final URI announceUri, final String requestQuery, final Iterable<Map.Entry<String, String>> headers) throws AnnounceException {
        final String base = announceUri + (announceUri.toString().contains("?") ? "&": "?");
        final Request request = Request.Get(base + requestQuery);

        String host = announceUri.getHost();
        if (announceUri.getPort() != -1) {
            host += ":" + announceUri.getPort();
        }
        request.addHeader("Host", host);
        headers.forEach(entry -> request.addHeader(entry.getKey(), entry.getValue()));

        final Response response;
        try {
            response = request.execute();
        } catch (final ClientProtocolException e) {
            throw new AnnounceException("Failed to announce: protocol mismatch.", e);
        } catch (final IOException e) {
            throw new AnnounceException("Failed to announce: error or connection aborted.", e);
        }

        try {
            return response.handleResponse(this.trackerResponseHandler);
        } catch (final IOException e) {
            throw new AnnounceException("Failed to handle tracker response: " + e.getMessage(), e);
        }
    }

}
