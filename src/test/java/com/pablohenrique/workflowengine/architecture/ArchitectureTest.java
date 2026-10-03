package com.pablohenrique.workflowengine.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Garante em build as restrições de docs/architecture/ARCHITECTURE.md (AC-001 a AC-005)
 * e as regras de dependência de docs/architecture/MODULES.md (MR-001 a MR-003).
 */
class ArchitectureTest {

    private static final String BASE = "com.pablohenrique.workflowengine";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domainAndContractsDoNotDependOnFrameworks() {
        noClasses().that().resideInAnyPackage("..domain..", "..contract..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..",
                        "com.fasterxml..", "io.swagger..")
                .because("o domínio deve existir sem Spring, JPA ou HTTP (AC-001)")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..",
                        "..interfaces..")
                .because("as dependências apontam para o domínio, nunca a partir dele")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnAdapters() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces..")
                .because("a aplicação define portas; a infraestrutura as implementa")
                .check(classes);
    }

    @Test
    void interfacesDoNotReachInfrastructure() {
        noClasses().that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("controllers falam com casos de uso, não com persistência (AC-004)")
                .check(classes);
    }

    @Test
    void contractsDoNotExposeModuleInternals() {
        noClasses().that().resideInAPackage("..contract..")
                .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..",
                        "..infrastructure..", "..interfaces..")
                .because("um contrato público não pode vazar a implementação do módulo (MR-003)")
                .check(classes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"definition", "execution", "rules", "audit", "identity"})
    void modulesAreOnlyAccessedThroughTheirContracts(String module) {
        String modulePackage = BASE + "." + module + "..";
        String contractPackage = BASE + "." + module + ".contract..";

        noClasses().that().resideOutsideOfPackage(modulePackage)
                .should().dependOnClassesThat(resideInAPackage(modulePackage).and(not(resideInAPackage(contractPackage))))
                .because("módulos só interagem por contratos explícitos (MR-002)")
                .check(classes);
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("dependências circulares entre módulos não são permitidas (MR-001)")
                .check(classes);
    }
}
