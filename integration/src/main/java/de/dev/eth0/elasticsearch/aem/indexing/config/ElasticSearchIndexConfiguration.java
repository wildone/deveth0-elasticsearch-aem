package de.dev.eth0.elasticsearch.aem.indexing.config;

import org.apache.jackrabbit.JcrConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to configure the ElasticSearch Index
 */
@Component(
        service = ElasticSearchIndexConfiguration.class,
        configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                ElasticSearchIndexConfiguration.PRIMARY_TYPE + "=${config.primaryType}",
                "indexRules=${config.indexRules}",
                "reindex=${config.reindex}"
        }
)
@Designate(ocd = ElasticSearchIndexConfiguration.Config.class, factory = true)
public class ElasticSearchIndexConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexConfiguration.class);

  public static final String PRIMARY_TYPE = JcrConstants.JCR_PRIMARYTYPE;
  public static final String SERVICE_NAME = "ElasticSearch Index Configuration";
  public static final String SERVICE_DESCRIPTION = "Service to configure the ElasticSearch Index";

  private String[] indexRules;

  @Activate
  protected void activate(Config config) {
    this.indexRules = config.indexRules();
    // Optional reindexing logic
    if (config.reindex()) {
      LOG.info("Reindexing triggered for primaryType: {}", config.primaryType());
      // TODO: trigger reindex logic (e.g., jobManager.schedule)
    }
  }

  public String[] getIndexRules() {
    return indexRules;
  }

  @ObjectClassDefinition(name = SERVICE_NAME, description = SERVICE_DESCRIPTION)
  public @interface Config {

    @AttributeDefinition(
            name = "Primary Type",
            description = "Primary Type for which this configuration is responsible. E.g. cq:Page or dam:Asset"
    )
    String primaryType();

    @AttributeDefinition(
            name = "Index Rules",
            description = "List of property names that should be indexed"
    )
    String[] indexRules() default {};

    @AttributeDefinition(
            name = "Reindex",
            description = "If enabled, a reindexing will start on save"
    )
    boolean reindex() default false;

    @AttributeDefinition(
            name = "Web Console Name Hint",
            description = "Shown in the web console config factory list"
    )
    String webconsole_configurationFactory_nameHint() default "Primary Type: {primaryType}";
  }
}
