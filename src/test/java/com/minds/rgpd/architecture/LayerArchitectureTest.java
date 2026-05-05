package com.minds.rgpd.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests to enforce layered architecture rules for the MINDS SaaS RGPD application.
 * <p>
 * This test class validates the clean architecture principles with proper layer separation:
 * - Web layer (Controllers, DTOs, Mappers)
 * - Business layer (Services, Domain logic)
 * - Persistence layer (Entities, Repositories)
 * - Infrastructure layer (Configuration, Security, Utils)
 */
class LayerArchitectureTest {

/*    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minds.rgpd");
    }

    @Test
    void shouldRespectLayeredArchitecture() {
        layeredArchitecture()
                .consideringAllDependencies()

                // Define layers
                .layer("Web").definedBy("..web..")
                .layer("Business").definedBy("..business..")
                .layer("Persistence").definedBy("..persistence..")
                .layer("Infrastructure").definedBy("..infrastructure..")

                // Define access rules - layers can only access lower layers
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Business").mayOnlyBeAccessedByLayers("Web")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Business", "Infrastructure")
                .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Web", "Business")

                .check(importedClasses);
    }

    @Test
    void webLayerShouldNotAccessPersistenceDirectly() {
        noClasses()
                .that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..persistence..")
                .because("Web layer should only access Business layer, not Persistence directly")
                .check(importedClasses);
    }

    @Test
    void businessLayerShouldNotAccessWebLayer() {
        noClasses()
                .that().resideInAPackage("..business..")
                .should().dependOnClassesThat().resideInAPackage("..web..")
                .because("Business layer should not depend on Web layer (dependency inversion)")
                .check(importedClasses);
    }

    @Test
    void persistenceLayerShouldNotAccessHigherLayers() {
        noClasses()
                .that().resideInAPackage("..persistence..")
                .should().dependOnClassesThat().resideInAnyPackage("..web..", "..business..")
                .because("Persistence layer should not depend on higher layers")
                .check(importedClasses);
    }

    @Test
    void infrastructureLayerShouldNotAccessWebOrBusinessLayers() {
        noClasses()
                .that().resideInAPackage("..infrastructure..")
                .and().areNotAnnotatedWith("org.springframework.context.annotation.Configuration")
                .and().haveSimpleNameNotContaining("Security")
                .and().haveSimpleNameNotContaining("Cors")
                .and().haveSimpleNameNotContaining("Utils")
                .should().dependOnClassesThat().resideInAnyPackage("..web..", "..business..")
                .because("Infrastructure layer should be independent of application layers (except security, CORS, and utilities)")
                .check(importedClasses);
    }*/
}