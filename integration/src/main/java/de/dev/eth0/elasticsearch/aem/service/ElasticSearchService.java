package de.dev.eth0.elasticsearch.aem.service;

import java.io.IOException;
import lombok.Getter;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service keeping a connection to the ElasticSearch host
 */
@Component(
        service = ElasticSearchService.class,
        immediate = true
)
public class ElasticSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchService.class);

  @Reference
  protected ElasticSearchHostConfiguration hostConfiguration;

  @Getter
  private RestClient restClient;

  @Activate
  public void activate() {
    restClient = RestClient.builder(
            new HttpHost(
                    hostConfiguration.getHost(),
                    hostConfiguration.getPort(),
                    hostConfiguration.getProtocol()
            )
    ).build();
  }

  @Deactivate
  public void deactivate() {
    if (this.restClient != null) {
      try {
        this.restClient.close();
      } catch (IOException ioe) {
        LOG.warn("Could not close ElasticSearch RestClient", ioe);
      }
    }
  }
}
