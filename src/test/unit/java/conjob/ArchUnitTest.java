package conjob;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "conjob")
public class ArchUnitTest {
    @Test
    @DisplayName("Given a class in the core library, " +
            "when it depends on another class, " +
            "then the dependency should be in the core library, " +
            "or the dependency should be an external dependency.")
    public void coreLibraryShouldOnlyDependOnItself() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("conjob..");

        classes()
                .that().resideInAPackage("conjob.core..")
                .should().onlyDependOnClassesThat(
                resideInAPackage("conjob.core..")
                        .or(doNot(resideInAPackage("conjob.."))))
                .because("lower level libraries shouldn't have dependencies higher level ones.")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Given a class in the service library, " +
            "when it depends on another class, " +
            "then the dependency should be in the same library " +
            "or the dependency should only be in lower level libraries, " +
            "or the dependency should be an external dependency.")
    public void serviceLibraryShouldOnlyDependOnItselfAndCore() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("conjob..");

        classes()
                .that().resideInAPackage("conjob.service..")
                .should().onlyDependOnClassesThat(
                resideInAnyPackage("conjob.service..", "conjob.core..", "conjob.config..")
                        .or(doNot(resideInAPackage("conjob.."))))
                .because("lower level libraries shouldn't have dependencies are higher level ones.")
                .check(importedClasses);
    }
}
