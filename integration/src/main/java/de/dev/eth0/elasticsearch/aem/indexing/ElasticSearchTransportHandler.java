package de.dev.eth0.elasticsearch.aem.indexing;

import com.day.cq.replication.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dev.eth0.elasticsearch.aem.service.ElasticSearchService;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONException;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransportHandler Implementation that posts a ReplicationContent created by {@link ElasticSearchIndexContentBuilder} to a configured ElasticSearch.
 */
@Component(
        service = TransportHandler.class,
        immediate = true,
        property = {
                "service.ranking:Integer=1000"
        }
)
public class ElasticSearchTransportHandler implements TransportHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchTransportHandler.class);

  @Reference
  protected ElasticSearchService elasticSearchService;

  @Override
  public boolean canHandle(AgentConfig config) {
    return StringUtils.equalsIgnoreCase(config.getSerializationType(), ElasticSearchIndexContentBuilder.NAME);
  }

  @Override
  public ReplicationResult deliver(TransportContext ctx, ReplicationTransaction tx) throws ReplicationException {
    ReplicationLog log = tx.getLog();
    try {
      RestClient restClient = elasticSearchService.getRestClient();
      ReplicationActionType replicationType = tx.getAction().getType();
      if (replicationType == ReplicationActionType.TEST) {
        return doTest(ctx, tx, restClient);
      } else {
        log.info(getClass().getSimpleName() + ": ---------------------------------------");
        if (tx.getContent() == ReplicationContent.VOID) {
          LOG.warn("No Replication Content provided");
          return new ReplicationResult(true, 0, "No Replication Content provided for path " + tx.getAction().getPath());
        }
        switch (replicationType) {
          case ACTIVATE:
            return doActivate(ctx, tx, restClient);
          case DEACTIVATE:
            return doDeactivate(ctx, tx, restClient);
          default:
            log.warn(getClass().getSimpleName() + ": Replication action type " + replicationType + " not supported.");
            throw new ReplicationException("Replication action type " + replicationType + " not supported.");
        }
      }
    } catch (JSONException jex) {
      LOG.error("JSON was invalid", jex);
      return new ReplicationResult(false, 0, jex.getLocalizedMessage());
    } catch (IOException ioe) {
      log.error(getClass().getSimpleName() + ": Could not perform Indexing due to " + ioe.getLocalizedMessage());
      LOG.error("Could not perform Indexing", ioe);
      return new ReplicationResult(false, 0, ioe.getLocalizedMessage());
    }
  }

  private ReplicationResult doDeactivate(TransportContext ctx, ReplicationTransaction tx, RestClient restClient)
          throws ReplicationException, JSONException, IOException {
    ReplicationLog log = tx.getLog();
    ObjectMapper mapper = new ObjectMapper();
    IndexEntry content = mapper.readValue(tx.getContent().getInputStream(), IndexEntry.class);
    Response deleteResponse = restClient.performRequest(
            new Request("DELETE", "/" + content.getIndex() + "/" + content.getType() + "/" + DigestUtils.md5Hex(content.getPath()))
    );
    LOG.debug(deleteResponse.toString());
    log.info(getClass().getSimpleName() + ": Delete Call returned " + deleteResponse.getStatusLine().getStatusCode() + ": " + deleteResponse.getStatusLine().getReasonPhrase());
    if (deleteResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED ||
            deleteResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      return ReplicationResult.OK;
    }
    LOG.error("Could not delete " + content.getType() + " at " + content.getPath());
    return new ReplicationResult(false, 0, "Replication failed");
  }

  /**
   * Perform the replication. All logic is covered in {@link ElasticSearchIndexContentBuilder} so we only need to transmit the JSON to ElasticSearch
   *
   * @param ctx
   * @param tx
   * @param restClient
   * @return
   * @throws ReplicationException
   */
  private ReplicationResult doActivate(TransportContext ctx, ReplicationTransaction tx, RestClient restClient)
          throws ReplicationException, JSONException, IOException {
    ReplicationLog log = tx.getLog();
    ObjectMapper mapper = new ObjectMapper();
    IndexEntry content = mapper.readValue(tx.getContent().getInputStream(), IndexEntry.class);
    if (content != null) {
      log.info(getClass().getSimpleName() + ": Indexing " + content.getPath());
      String contentString = mapper.writeValueAsString(content.getContent());
      log.debug(getClass().getSimpleName() + ": Index-Content: " + contentString);
      LOG.debug("Index-Content: " + contentString);

      HttpEntity entity = new NStringEntity(contentString, ContentType.APPLICATION_JSON);
      Response indexResponse = restClient.performRequest(
              new Request("PUT", "/" + content.getIndex() + "/" + content.getType() + "/" + DigestUtils.md5Hex(content.getPath()))
      );
      LOG.debug(indexResponse.toString());
      log.info(getClass().getSimpleName() + ": " + indexResponse.getStatusLine().getStatusCode() + ": " + indexResponse.getStatusLine().getReasonPhrase());
      if (indexResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED ||
              indexResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return ReplicationResult.OK;
      }
    }
    LOG.error("Could not replicate");
    return new ReplicationResult(false, 0, "Replication failed");
  }

  /**
   * Test Connection to ElasticSearch by getting basic informations
   *
   * @param ctx
   * @param tx
   * @param restClient
   * @return
   * @throws ReplicationException
   */
  private ReplicationResult doTest(TransportContext ctx, ReplicationTransaction tx, RestClient restClient)
          throws ReplicationException, IOException {
    ReplicationLog log = tx.getLog();
    Request request = new Request("GET", "/_cluster/health");
    request.addParameter("pretty", "true");
    Response response = restClient.performRequest(request);
    log.info(getClass().getSimpleName() + ": ---------------------------------------");
    log.info(getClass().getSimpleName() + ": " + response.toString());
    log.info(getClass().getSimpleName() + ": " + EntityUtils.toString(response.getEntity()));
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      return ReplicationResult.OK;
    }
    return new ReplicationResult(false, 0, "Replication test failed");
  }
}
