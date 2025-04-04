package de.dev.eth0.elasticsearch.aem.service;

import lombok.Getter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        service = ElasticSearchHostConfiguration.class,
        immediate = true,
        configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ElasticSearchHostConfiguration.Config.class, factory = true)
public class ElasticSearchHostConfiguration {

  public static final String SERVICE_NAME = "ElasticSearch Search Provider";
  public static final String SERVICE_DESCRIPTION = "Configuration for the ElasticSearch Search Provider";

  @ObjectClassDefinition(name = SERVICE_NAME, description = SERVICE_DESCRIPTION)
  public @interface Config {

    @AttributeDefinition(
            name = "Host",
            description = "Hostname of the ElasticSearch instance"
    )
    String host() default "localhost";

    @AttributeDefinition(
            name = "Port",
            description = "Port of the ElasticSearch (default: 9200)"
    )
    int port() default 9200;

    @AttributeDefinition(
            name = "Protocol",
            description = "Protocol for Elasticsearch"
    )
    String protocol() default "http";

    @AttributeDefinition(
            name = "Web Console Name Hint",
            description = "Shown in the OSGi console to identify this factory config"
    )
    String webconsole_configurationFactory_nameHint() default "Host: {host}";
  }

  @Getter
  protected String protocol;
  @Getter
  protected String host;
  @Getter
  protected int port;

  @Activate
  public void activate(Config config) {
    this.host = config.host();
    this.port = config.port();
    this.protocol = config.protocol();
  }
}
