package org.nuxeo.ecm.platform.picture.recompute;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.platform.picture.listener.PictureViewsGenerationListener.DISABLE_PICTURE_VIEWS_GENERATION_LISTENER;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.SetPropertyComputation;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

public class RecomputeViewsAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "recomputeViews";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(RecomputeViewsComputation::new, //
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class RecomputeViewsComputation extends AbstractBulkComputation {

        public static final String PICTURE_VIEWS_GENERATION_DONE_EVENT = "pictureViewsGenerationDone";

        private static final Logger log = LogManager.getLogger(SetPropertyComputation.class);

        public RecomputeViewsComputation() {
            super(ACTION_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {

            String xpath = "file:content";

            for (String docId : ids) {

                DocumentModel workingDocument = session.getDocument(new IdRef(docId));
                Property fileProp = workingDocument.getProperty(xpath);
                Blob blob = (Blob) fileProp.getValue();
                if (blob == null) {
                    // do nothing
                    return;
                }

                String title = workingDocument.getTitle();
                try {
                    PictureResourceAdapter picture = workingDocument.getAdapter(PictureResourceAdapter.class);
                    picture.fillPictureViews(blob, blob.getFilename(), title, null);
                } catch (DocumentNotFoundException e) {
                    // a parent of the document may have been deleted.
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (!session.exists(new IdRef(docId))) {
                    return;
                }
                if (workingDocument.isVersion()) {
                    workingDocument.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
                }
                workingDocument.putContextData("disableNotificationService", Boolean.TRUE);
                workingDocument.putContextData("disableAuditLogger", Boolean.TRUE);
                workingDocument.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
                workingDocument.putContextData(DISABLE_PICTURE_VIEWS_GENERATION_LISTENER, Boolean.TRUE);
                session.saveDocument(workingDocument);

                firePictureViewsGenerationDoneEvent(workingDocument, session);
            }
        }

        /**
         * Fire a {@code PICTURE_VIEWS_GENERATION_DONE_EVENT} event when no other PictureViewsGenerationWork is
         * scheduled for this document.
         *
         * @since 5.8
         */
        protected void firePictureViewsGenerationDoneEvent(DocumentModel doc, CoreSession session) {
            // WorkManager workManager = Framework.getService(WorkManager.class);
            // List<String> workIds = workManager.listWorkIds(CATEGORY_PICTURE_GENERATION, null);
            // int worksCount = 0;
            // for (String workId : workIds) {
            // if (workId.equals(getId())) {
            // if (++worksCount > 1) {
            // // another work scheduled
            // return;
            // }
            // }
            // }
            DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
            Event event = ctx.newEvent(PICTURE_VIEWS_GENERATION_DONE_EVENT);
            Framework.getService(EventService.class).fireEvent(event);
        }

    }
}
