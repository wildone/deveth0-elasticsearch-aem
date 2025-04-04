package de.dev.eth0.elasticsearch.aem.indexing.contentbuilder;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
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
 * Content Builder for CQ Pages
 */
@Component(
        service = ElasticSearchContentBuilder.class,
        immediate = true,
        property = {
                ElasticSearchIndexConfiguration.PRIMARY_TYPE + "primaryType=" + PageContentBuilder.PRIMARY_TYPE_VALUE
        }
)
public class PageContentBuilder extends AbstractElasticSearchContentBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(PageContentBuilder.class);

  public static final String PRIMARY_TYPE_VALUE = "cq:Page";

  private static final String[] FIXED_RULES = {
          "cq:lastModified", "cq:template", "jcr:description", "jcr:title"
  };

  @Override
  public IndexEntry create(String path, @Nonnull ResourceResolver resolver) {
    String[] indexRules = getIndexRules(PRIMARY_TYPE_VALUE);
    if (ArrayUtils.isNotEmpty(indexRules)) {
      PageManager pageManager = resolver.adaptTo(PageManager.class);
      if (pageManager != null) {
        Page page = pageManager.getPage(path);
        if (page != null) {
          IndexEntry ret = new IndexEntry("idx", "page", path);
          Resource res = page.getContentResource();
          if (res != null) {
            ret.addContent(getProperties(res, indexRules));
          }
          return ret;
        }
      }
    } else {
      LOG.warn("Could not load indexRules for {}", PRIMARY_TYPE_VALUE);
    }
    return null;
  }

  @Override
  protected String[] getFixedRules() {
    return FIXED_RULES;
  }
}
