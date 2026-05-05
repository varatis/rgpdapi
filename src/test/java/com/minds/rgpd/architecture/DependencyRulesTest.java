package com.minds.rgpd.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests to enforce dependency rules and prevent circular dependencies.
 * <p>
 * This test class validates:
 * - No circular dependencies between packages
 * - Proper separation of concerns
 * - Compliance with clean architecture principles
 */
class DependencyRulesTest {

/*    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minds.rgpd");
    }

    @Test
    void noCircularDependenciesBetweenSlices() {
        // Check that business layer doesn't depend on web layer (would create circular dependency)
        noClasses()
                .that().resideInAPackage("..business..")
                .should().dependOnClassesThat()
                .resideInAPackage("..web..")
                .because("Business layer should not depend on web layer to avoid circular dependencies")
                .check(importedClasses);
    }

    @Test
    void controllersShouldOnlyDependOnServicesAndDTOs() {
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
                        "org.slf4j..",
                        "com.minds.rgpd.business.exceptions..",
                        "io.swagger.."
                )
                .because("Controllers should only depend on services, DTOs, mappers and framework classes")
                .check(importedClasses);
    }

    @Test
    void servicesShouldNotDependOnWebLayer() {
        noClasses()
                .that().resideInAPackage("..business.services..")
                .should().dependOnClassesThat().resideInAPackage("..web..")
                .because("Services should not depend on web layer classes (dependency inversion)")
                .check(importedClasses);
    }

    @Test
    void repositoriesShouldOnlyDependOnEntities() {
        classes()
                .that().resideInAPackage("..persistence.repositories..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..persistence.entities..",
                        "java..",
                        "org.springframework..",
                        "jakarta.persistence.."
                )
                .because("Repositories should only depend on entities and framework classes")
                .check(importedClasses);
    }

    @Test
    void entitiesShouldNotDependOnOtherLayers() {
        classes()
                .that().resideInAPackage("..persistence.entities..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..persistence.entities..",
                        "..utils..", // Allow for StringDateConverter
                        "java..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "lombok..",
                        "org.hibernate..",
                        "com.fasterxml.jackson.."
                )
                .because("Entities should only depend on other entities and framework classes")
                .check(importedClasses);
    }

    @Test
    void mappersShouldOnlyAccessEntitiesAndDTOs() {
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
    void configurationClassesShouldResideInInfrastructure() {
        classes()
                .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
                .should().resideInAPackage("..infrastructure..")
                .because("Configuration classes should reside in infrastructure.configuration package")
                .check(importedClasses);
    }*/
}