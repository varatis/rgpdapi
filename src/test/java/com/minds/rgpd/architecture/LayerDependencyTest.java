package com.minds.rgpd.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Comprehensive ArchUnit tests to enforce clean architecture layer dependencies.
 * These tests validate the critical architectural rule violations identified:
 * 1. Controllers should not directly depend on persistence entities
 * 2. Services should not depend on web layer (DTOs, controllers, mappers)
 * 3. Proper dependency direction enforcement
 */
class LayerDependencyTest {

/*    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minds.rgpd");
    }

    @Test
    void controllersShouldNotDependOnPersistenceEntities() {
        noClasses()
                .that().resideInAPackage("..web.controllers..")
                .should().dependOnClassesThat().resideInAPackage("..persistence.entities..")
                .because("Controllers should not directly access persistence entities - use DTOs instead")
                .check(importedClasses);
    }

    @Test
    void servicesShouldNotDependOnWebLayer() {
        noClasses()
                .that().resideInAPackage("..business.services..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..web.dto..",
                        "..web.controllers..",
                        "..web.mappers.."
                )
                .because("Services should not depend on web layer - this violates dependency inversion principle")
                .check(importedClasses);
    }

    @Test
    void persistenceLayerShouldNotDependOnBusinessOrWebLayers() {
        noClasses()
                .that().resideInAPackage("..persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..business..",
                        "..web.."
                )
                .because("Persistence layer should only depend on infrastructure and framework classes")
                .check(importedClasses);
    }

    @Test
    void webLayerShouldOnlyDependOnBusinessLayer() {
        classes()
                .that().resideInAPackage("..web.controllers..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..business.services..",
                        "..business.dto..",
                        "java..",
                        "org.springframework..",
                        "lombok..",
                        "jakarta.validation..",
                        "jakarta.servlet..",
                        "org.slf4j..",
                        "com.minds.rgpd.business.exceptions..",
                        "io.swagger.."
                )
                .because("Controllers should only depend on services, DTOs, mappers and framework classes")
                .check(importedClasses);
    }

    @Test
    void mappersShouldBridgeBusinessAndPersistenceLayers() {
        classes()
                .that().resideInAPackage("..business.utilities.mappers..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..business.dto..",
                        "..business.utilities.mappers..",
                        "..persistence.entities..",
                        "java..",
                        "org.springframework..",
                        "org.mapstruct..",
                        "lombok.."
                )
                .because("Mappers should only access DTOs, entities and framework classes")
                .check(importedClasses);
    }

    @Test
    void businessLayerShouldOnlyDependOnPersistenceAndInfrastructure() {
        classes()
                .that().resideInAPackage("..business.services..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..business..",
                        "..persistence..",
                        "..infrastructure..",
                        "java..",
                        "org.springframework..",
                        "lombok..",
                        "org.slf4j..",
                        "io.micrometer..",
                        "..utils.."
                )
                .because("Business layer should only depend on persistence, infrastructure and framework classes")
                .check(importedClasses);
    }

    @Test
    void infrastructureLayerShouldNotDependOnOtherApplicationLayers() {
        noClasses()
                .that().resideInAPackage("..infrastructure..")
                .and().haveSimpleNameNotContaining("Security")
                .and().haveSimpleNameNotContaining("Cors")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..web..",
                        "..business..",
                        "..persistence.."
                )
                .because("Infrastructure should be independent (except security/CORS which need Spring Web)")
                .check(importedClasses);
    }*/
}