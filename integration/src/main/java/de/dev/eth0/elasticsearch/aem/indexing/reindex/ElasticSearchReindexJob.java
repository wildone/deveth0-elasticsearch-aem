package de.dev.eth0.elasticsearch.aem.indexing.reindex;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that performs a reindexing
 */
@Component(
        service = Runnable.class,
        immediate = true
)
public class ElasticSearchReindexJob implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchReindexJob.class);

  @Override
  public void run() {
    LOG.info("Running reindexing");
  }
}
