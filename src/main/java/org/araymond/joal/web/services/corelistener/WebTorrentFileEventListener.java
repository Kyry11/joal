package org.araymond.joal.web.services.corelistener;

import org.araymond.joal.core.events.torrent.files.FailedToAddTorrentFileEvent;
import org.araymond.joal.core.events.torrent.files.TorrentFileAddedEvent;
import org.araymond.joal.core.events.torrent.files.TorrentFileDeletedEvent;
import org.araymond.joal.web.annotations.ConditionalOnWebUi;
import org.araymond.joal.web.messages.outgoing.impl.files.FailedToAddTorrentFilePayload;
import org.araymond.joal.web.messages.outgoing.impl.files.TorrentFileAddedPayload;
import org.araymond.joal.web.messages.outgoing.impl.files.TorrentFileDeletedPayload;
import org.araymond.joal.web.services.JoalMessageSendingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Created by raymo on 11/07/2017.
 */
@ConditionalOnWebUi
@Service
public class WebTorrentFileEventListener extends WebEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebTorrentFileEventListener.class);

    @Inject
    public WebTorrentFileEventListener(final JoalMessageSendingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener
    void torrentFileAdded(final TorrentFileAddedEvent event) {
        logger.debug("Send TorrentFileAddedPayload to clients.");

        this.messagingTemplate.convertAndSend("/torrents", new TorrentFileAddedPayload(event));
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener
    void torrentFileDeleted(final TorrentFileDeletedEvent event) {
        logger.debug("Send TorrentFileDeletedPayload to clients.");

        this.messagingTemplate.convertAndSend("/torrents", new TorrentFileDeletedPayload(event));
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener
    void failedToAddTorrentFile(final FailedToAddTorrentFileEvent event) {
        logger.debug("Send FailedToAddTorrentFilePayload to clients.");

        this.messagingTemplate.convertAndSend("/torrents", new FailedToAddTorrentFilePayload(event));
    }

}
