package de.dev.eth0.elasticsearch.aem.indexing.contentbuilder;

import de.dev.eth0.elasticsearch.aem.indexing.config.ElasticSearchIndexConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractElasticSearchContentBuilder implements ElasticSearchContentBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractElasticSearchContentBuilder.class);

  private BundleContext context;

  @Activate
  public void activate(BundleContext context) {
    this.context = context;
  }

  /**
   * @param primaryType the JCR primary type
   * @return index rules for this type merged with fixed rules
   */
  protected String[] getIndexRules(String primaryType) {
    ElasticSearchIndexConfiguration config = getIndexConfig(primaryType);
    if (config != null) {
      return ArrayUtils.addAll(config.getIndexRules(), getFixedRules());
    }
    return getFixedRules();
  }

  /**
   * @return fixed rules to always include
   */
  protected String[] getFixedRules() {
    return new String[0];
  }

  /**
   * Service lookup for a configuration matching the given primaryType
   */
  private ElasticSearchIndexConfiguration getIndexConfig(String primaryType) {
    try {
      // find service for this primaryType
      ServiceReference<?>[] serviceReferences = context.getServiceReferences(
              ElasticSearchIndexConfiguration.class.getName(),
              "(" + ElasticSearchIndexConfiguration.PRIMARY_TYPE + "=" + primaryType + ")"
      );
      if (serviceReferences != null && serviceReferences.length > 0) {
        return (ElasticSearchIndexConfiguration) context.getService(serviceReferences[0]);
      }
    } catch (InvalidSyntaxException | NullPointerException ex) {
      LOG.error("Exception during service lookup", ex);
    }
    LOG.info("Could not load an ElasticSearchConfiguration for primaryType {}", primaryType);
    return null;
  }

  /**
   * Recursively fetches all matching properties from a resource and its children.
   */
  protected Map<String, Object> getProperties(Resource res, String[] properties) {
    ValueMap vm = res.getValueMap();
    Map<String, Object> ret = Arrays.stream(properties)
            .filter(vm::containsKey)
            .collect(Collectors.toMap(Function.identity(), vm::get));

    for (Resource child : res.getChildren()) {
      Map<String, Object> props = getProperties(child, properties);
      props.forEach((key, value) -> {
        if (!ret.containsKey(key)) {
          ret.put(key, value);
        } else {
          ret.put(key, mergeProperties(ret.get(key), value));
        }
      });
    }
    return ret;
  }

  private Object[] mergeProperties(Object obj1, Object obj2) {
    List<Object> merged = new ArrayList<>();
    addProperty(merged, obj1);
    addProperty(merged, obj2);
    return merged.toArray();
  }

  private void addProperty(List<Object> list, Object property) {
    if (property != null) {
      if (property.getClass().isArray()) {
        list.addAll(Arrays.asList((Object[]) property));
      } else {
        list.add(property);
      }
    }
  }
}
