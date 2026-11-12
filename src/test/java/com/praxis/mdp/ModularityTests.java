package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  private final ApplicationModules modules =
      ApplicationModules.of(MarketDataPublisherApplication.class);

  @Test
  void verifyModularStructure() {
    modules.verify();
    assertThat(modules.getModuleByName("domain").orElseThrow().isOpen()).isTrue();
    assertThat(modules.getModuleByName("infrastructure").orElseThrow().isOpen()).isTrue();
    assertThat(
            modules
                .getModuleByName("domain")
                .orElseThrow()
                .getDirectDependencies(modules)
                .isEmpty())
        .isTrue();
    assertThat(
            modules
                .getModuleByName("scheduling")
                .orElseThrow()
                .getDirectDependencies(modules)
                .containsModuleNamed("run"))
        .isFalse();
    assertThat(
            modules
                .getModuleByName("pipeline")
                .orElseThrow()
                .getDirectDependencies(modules)
                .containsModuleNamed("run"))
        .isFalse();
    assertThat(
            modules
                .getModuleByName("source")
                .orElseThrow()
                .getDirectDependencies(modules)
                .containsModuleNamed("pipeline"))
        .isFalse();
    assertThat(
            modules
                .getModuleByName("publishing")
                .orElseThrow()
                .getDirectDependencies(modules)
                .containsModuleNamed("pipeline"))
        .isFalse();
  }

  @Test
  void writeModuleDocumentation() {
    new Documenter(modules)
        .writeDocumentation()
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml();
    assertThat(java.nio.file.Path.of("target/spring-modulith-docs")).isDirectory();
  }
}
