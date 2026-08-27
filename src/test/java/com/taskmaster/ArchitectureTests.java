package com.taskmaster;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architectural fitness tests enforcing Hexagonal (Ports & Adapters) boundaries and modular monolith isolation.
 */
@AnalyzeClasses(
    packages = "com.taskmaster",
    importOptions = {ImportOption.DoNotIncludeTests.class}
)
class ArchitectureTests {

    @ArchTest
    static final ArchRule domainShouldNotDependOnAdapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationServicesShouldNotDependOnInboundAdapters =
        noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.in..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inboundAdaptersShouldNotDependOnOutboundAdapters =
        noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainPortsShouldNotDependOnApplicationServices =
        noClasses()
            .that().resideInAPackage("..domain.port..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application.service..")
            .allowEmptyShould(true);
}
