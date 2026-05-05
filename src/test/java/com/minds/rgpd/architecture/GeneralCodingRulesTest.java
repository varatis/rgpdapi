package com.minds.rgpd.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

/**
 * ArchUnit tests for general coding rules and best practices.
 * <p>
 * This test class enforces:
 * - General coding best practices
 * - Proper exception handling
 * - Access modifier usage
 * - Framework-specific rules
 */
class GeneralCodingRulesTest {

/*    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.minds.rgpd");
    }

    @Test
    void noGenericExceptions() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
                .because("Generic exceptions should not be thrown")
                .check(importedClasses);
    }

    @Test
    void noJavaUtilLogging() {
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
                .because("Use SLF4J instead of java.util.logging")
                .check(importedClasses);
    }

    @Test
    @Disabled("Need to fix the issues")
    void noSystemOutOrErr() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .because("Use proper logging instead of System.out/System.err")
                .check(importedClasses);
    }

    @Test
    void fieldInjectionShouldBeAvoided() {
        noFields()
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .because("Field injection should be avoided. Use constructor injection instead")
                .check(importedClasses);
    }

    @Test
    @Disabled("Need to fix the issues")
    void servicesShouldNotBePublic() {
        noClasses()
                .that().resideInAPackage("..business.services.impl..")
                .and().areNotInterfaces()
                .should().bePublic()
                .because("Service implementations should use package-private visibility when possible")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    void repositoriesShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..persistence.repositories..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .because("Repositories should be interfaces extending JpaRepository")
                .check(importedClasses);
    }

    @Test
    void businessExceptionsShouldExtendRuntimeException() {
        classes()
                .that().resideInAPackage("..business.exceptions..")
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(RuntimeException.class)
                .because("Business exceptions should extend RuntimeException")
                .check(importedClasses);
    }

    @Test
    void controllersShouldNotHaveState() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..web.controllers..")
                .and().areNotStatic()
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .orShould().beFinal()
                .because("Controllers should be stateless - only dependency injection or constants allowed")
                .check(importedClasses);
    }

    @Test
    void servicesShouldNotHaveState() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("..business.services..")
                .and().areNotStatic()
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .orShould().beFinal()
                .because("Services should be stateless - only dependency injection or constants allowed")
                .check(importedClasses);
    }

    @Test
    void utilitiesShouldBeFinalAndHavePrivateConstructor() {
        classes()
                .that().resideInAPackage("..utils..")
                .and().areNotAssignableTo("jakarta.persistence.AttributeConverter")
                .should().haveOnlyPrivateConstructors()
                .because("Utility classes should have private constructors (except JPA converters)")
                .allowEmptyShould(true)  // Allow empty if no utility classes exist
                .check(importedClasses);
    }

    @Test
    void entitiesShouldHaveNoArgsConstructor() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().haveOnlyPrivateConstructors()
                .orShould().bePublic()
                .because("JPA entities should have a no-args constructor (can be private with Lombok)")
                .check(importedClasses);
    }*/
}