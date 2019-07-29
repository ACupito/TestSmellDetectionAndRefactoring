package main.refactor.strategy;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.extractclass.ExtractClassProcessor;
import it.unisa.testSmellDiffusion.beans.ClassBean;
import it.unisa.testSmellDiffusion.beans.InstanceVariableBean;
import it.unisa.testSmellDiffusion.beans.MethodBean;
import it.unisa.testSmellDiffusion.beans.PackageBean;
import it.unisa.testSmellDiffusion.testSmellInfo.lackOfCohesion.LackOfCohesionInfo;
import main.refactor.IRefactor;
import org.jsoup.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class LackOfCohesionStrategy implements IRefactor {
    private Project project;
    private LackOfCohesionInfo informations;

    public LackOfCohesionStrategy(LackOfCohesionInfo informations, Project project){
        this.informations = informations;
        this.project = project;
    }

    @Override
    public void doRefactor() {
        ExtractClassProcessor processor;

        List<PsiMethod> methodsToMove = new ArrayList<>();
        List<PsiField> fieldsToMove = new ArrayList<>();
        List<PsiClass> innerClasses = new ArrayList<>();

        ClassBean originalClass = informations.getTestClass();
        PsiClass psiOriginalClass = PsiUtil.getPsi(originalClass, project);
        String packageName = originalClass.getBelongingPackage();

        PackageBean packageBean = new PackageBean();
        packageBean.setName(packageName);
        packageBean.addClass(originalClass);
        Vector<PackageBean> packs = new Vector<>();
        packs.add(packageBean);

        informations.checkMethodsThatCauseLackOfCohesion(packs); //PROBLEMINO DA SEGNALARE
        ArrayList<MethodBean> infectedMethodList = informations.getMethodsThatCauseLackOfCohesion(); //sempre vuoto capire il perchè

        if(infectedMethodList.size() > 0) {

            for (MethodBean infect : infectedMethodList) {

                PsiMethod psiInfect = PsiUtil.getPsi(infect, project, psiOriginalClass);
                methodsToMove.add(psiInfect);

                Vector<InstanceVariableBean> instances = (Vector<InstanceVariableBean>) infect.getUsedInstanceVariables();
                if (instances.size() > 0) {
                    for (int i = 0; i < instances.size(); i++) {
                        fieldsToMove.add(psiOriginalClass.findFieldByName(instances.get(i).getName(), true));
                    }
                }
            }

        }
        for(PsiClass innerClass: psiOriginalClass.getInnerClasses()){
            innerClasses.add(innerClass);
        }

        String classShortName = "Refactored"+originalClass.getName();

        processor = new ExtractClassProcessor(
                psiOriginalClass,
                fieldsToMove,
                methodsToMove,
                innerClasses,
                packageName,
                classShortName);

        processor.run();
    }
}
