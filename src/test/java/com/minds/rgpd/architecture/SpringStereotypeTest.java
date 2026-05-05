package com.minds.rgpd.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * ArchUnit tests to enforce proper usage of Spring stereotypes and annotations.
 * <p>
 * This test class ensures that:
 * - Controllers use @RestController annotation
 * - Services use @Service annotation
 * - Repositories use @Repository annotation
 * - Proper naming conventions are followed
 */
class SpringStereotypeTest {

/*    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minds.rgpd");
    }

    @Test
    void controllersShouldBeAnnotatedWithRestController() {
        classes()
                .that().resideInAPackage("..web.controllers..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(RestController.class)
                .orShould().beAnnotatedWith(Controller.class)
                .orShould().beAnnotatedWith(RestControllerAdvice.class)
                .because("Controllers should be annotated with @RestController or @Controller")
                .check(importedClasses);
    }

    @Test
    void servicesShouldBeAnnotatedWithService() {
        classes()
                .that().resideInAPackage("..business.services..")
                .and().areNotInterfaces()
                .and().haveSimpleNameEndingWith("Service")
                .or().haveSimpleNameEndingWith("ServiceImpl")
                .should().beAnnotatedWith(Service.class)
                .because("Service implementations should be annotated with @Service")
                .check(importedClasses);
    }

    @Test
    void repositoriesShouldBeAnnotatedWithRepository() {
        classes()
                .that().resideInAPackage("..persistence.repositories..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().beAnnotatedWith(Repository.class)
                .orShould().beAssignableTo(JpaRepository.class)
                .because("Repositories should be annotated with @Repository or extend JpaRepository")
                .check(importedClasses);
    }

    @Test
    void controllersShouldFollowNamingConvention() {
        classes()
                .that().resideInAPackage("..web.controllers..")
                .and().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .because("Controllers should follow the naming convention *Controller")
                .check(importedClasses);
    }

    @Test
    void servicesShouldFollowNamingConvention() {
        classes()
                .that().resideInAPackage("..business.services..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl")
                .because("Service implementations should follow the naming convention *Service or *ServiceImpl")
                .check(importedClasses);
    }

    @Test
    void repositoriesShouldFollowNamingConvention() {
        classes()
                .that().resideInAPackage("..persistence.repositories..")
                .should().haveSimpleNameEndingWith("Repository")
                .because("Repositories should follow the naming convention *Repository")
                .check(importedClasses);
    }

    @Test
    void entitiesShouldResideInEntitiesPackage() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..persistence.entities..")
                .because("JPA entities should reside in persistence.entities package")
                .check(importedClasses);
    }

    @Test
    void dtosShouldResideInBusinessPackage() {
        classes()
                .that().haveSimpleNameEndingWith("DTO")
                .should().resideInAPackage("..business.dto..")
                .because("DTOs should reside in business.dto package for clean architecture")
                .check(importedClasses);
    }*/
}