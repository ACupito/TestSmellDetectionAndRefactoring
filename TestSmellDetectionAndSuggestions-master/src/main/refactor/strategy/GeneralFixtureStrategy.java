package main.refactor.strategy;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.extractclass.ExtractClassProcessor;
import it.unisa.testSmellDiffusion.beans.ClassBean;
import it.unisa.testSmellDiffusion.beans.InstanceVariableBean;
import it.unisa.testSmellDiffusion.beans.MethodBean;
import it.unisa.testSmellDiffusion.testSmellInfo.generalFixture.GeneralFixtureInfo;
import it.unisa.testSmellDiffusion.testSmellInfo.generalFixture.MethodWithGeneralFixture;
import main.refactor.IRefactor;

import java.util.ArrayList;

import java.util.List;
import java.util.Vector;

public class GeneralFixtureStrategy implements IRefactor {
    private GeneralFixtureInfo generalFixtureInfo;
    private MethodWithGeneralFixture methodWithGeneralFixture;
    private Project project;
    private ExtractClassProcessor processor;

    public GeneralFixtureStrategy(MethodWithGeneralFixture methodWithGeneralFixture, Project project, GeneralFixtureInfo generalFixtureInfo){
        this.methodWithGeneralFixture = methodWithGeneralFixture;
        this.project = project;
        this.generalFixtureInfo = generalFixtureInfo;
    }

    @Override
    public void doRefactor() {
        ClassBean originalClass = generalFixtureInfo.getTestClass();
        PsiClass psiOriginalClass = PsiUtil.getPsi(originalClass, project);
        String packageName = originalClass.getBelongingPackage();

        List<PsiMethod> methodsToMove = new ArrayList<>();
        List<PsiField> fieldsToMove = new ArrayList<>();
        List<PsiClass> innerClasses = new ArrayList<>();

        MethodBean method = methodWithGeneralFixture.getMethod();
        PsiMethod infectedMethod = PsiUtil.getPsi(method, project, psiOriginalClass);
        methodsToMove.add(infectedMethod);

        Vector<InstanceVariableBean> instances = (Vector<InstanceVariableBean>) method.getUsedInstanceVariables();
        if(instances.size() > 0) {

            for (int i = 0; i < instances.size(); i++) {
                fieldsToMove.add(psiOriginalClass.findFieldByName(instances.get(i).getName(), true));
            }

            /* Utile per spostare il metodo setup nella nuova classe NON FUNZIONA ANCORA
            PsiMethod[] setupList = psiOriginalClass.findMethodsByName("setUp()", true);
            if(setupList.length != 0){
                PsiMethod setup = setupList[0];
                methodsToMove.add(setup);
            }*/
        }
        for(PsiClass innerClass: psiOriginalClass.getInnerClasses()){
            innerClasses.add(innerClass);
        }

        String classShortName = "Refactored"+originalClass.getName()+infectedMethod.getName();

        processor = new ExtractClassProcessor(
                psiOriginalClass,
                fieldsToMove,
                methodsToMove,
                innerClasses,
                packageName,
                classShortName);

        processor.run();

        //utile per l'eliminazione del costruttore nel nuovo metodo creato NON FUNZIONA
        /*PsiMethod[] constructors = processor.getCreatedClass().getConstructors();
        PsiMethod constructor = constructors[0];
        constructor.delete();*/

    }
}
