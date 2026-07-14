package com.example.attendance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.attendance");

    @Test
    @DisplayName("Controller は Service のみに依存し Repository を直接使わない")
    void controllers_shouldNotDependOnRepositories() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Entity は他のレイヤーに依存しない")
    void entities_shouldNotDependOnOtherLayers() {
        noClasses().that().resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..controller..", "..service..", "..repository..", "..config.."
                )
                .check(importedClasses);
    }

    @Test
    @DisplayName("Service 実装は controller に依存しない")
    void services_shouldNotDependOnControllers() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .check(importedClasses);
    }
}
