package com.praxis.mdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

@Modulithic(
    systemName = "Market Data Publisher",
    sharedModules = {"domain", "infrastructure"})
@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@ConfigurationPropertiesScan
public class MarketDataPublisherApplication {

  public static void main(String[] args) {
    SpringApplication.run(MarketDataPublisherApplication.class, args);
  }
}
