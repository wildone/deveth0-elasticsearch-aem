package de.dev.eth0.elasticsearch.aem.indexing.contentbuilder;

import com.day.cq.dam.api.Asset;
import de.dev.eth0.elasticsearch.aem.indexing.IndexEntry;
import de.dev.eth0.elasticsearch.aem.indexing.config.ElasticSearchIndexConfiguration;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content Builder for DAM Assets
 */
@Component(
        service = ElasticSearchContentBuilder.class,
        immediate = true,
        property = {
                ElasticSearchIndexConfiguration.PRIMARY_TYPE + "=" + DAMAssetContentBuilder.PRIMARY_TYPE_VALUE
        }
)
public class DAMAssetContentBuilder extends AbstractElasticSearchContentBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DAMAssetContentBuilder.class);

  public static final String PRIMARY_TYPE_VALUE = "dam:Asset";

  private static final String[] FIXED_RULES = {
          "dc:title", "dc:description", "jcr:lastModified"
  };

  @Override
  public IndexEntry create(String path, @Nonnull ResourceResolver resolver) {
    String[] indexRules = getIndexRules(PRIMARY_TYPE_VALUE);
    if (ArrayUtils.isNotEmpty(indexRules)) {
      Resource res = resolver.getResource(path);
      if (res != null) {
        Asset asset = res.adaptTo(Asset.class);
        if (asset != null) {
          IndexEntry ret = new IndexEntry("idx", "asset", path);
          ret.addContent(getProperties(res, indexRules));
          return ret;
        }
        LOG.error("Could not adapt asset");
      }
    }
    return null;
  }

  @Override
  protected String[] getFixedRules() {
    return FIXED_RULES;
  }
}
