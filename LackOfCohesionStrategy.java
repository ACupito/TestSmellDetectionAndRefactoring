package main.refactor.strategy;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.refactoring.extractclass.ExtractClassProcessor;
import it.unisa.testSmellDiffusion.beans.ClassBean;
import it.unisa.testSmellDiffusion.beans.InstanceVariableBean;
import it.unisa.testSmellDiffusion.beans.MethodBean;
import it.unisa.testSmellDiffusion.testSmellInfo.lackOfCohesion.LackOfCohesionInfo;
import main.refactor.IRefactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class LackOfCohesionStrategy implements IRefactor {
    private Project project;
    private LackOfCohesionInfo informations;
    private Editor editor;

    public LackOfCohesionStrategy(LackOfCohesionInfo informations, Project project) {
        this.informations = informations;
        this.project = project;
    }

    @Override
    public void doRefactor() throws PrepareFailedException {
        ExtractClassProcessor processor;

        List<PsiMethod> methodsToMove = new ArrayList<>();
        List<PsiField> fieldsToMove = new ArrayList<>();
        List<PsiClass> innerClasses = new ArrayList<>();

        ClassBean originalClass = informations.getTestClass();
        PsiClass psiOriginalClass = PsiUtil.getPsi(originalClass, project);
        String packageName = originalClass.getBelongingPackage();
/*
        PackageBean packageBean = new PackageBean();
        packageBean.setName(packageName);
        packageBean.addClass(originalClass);
        Vector<PackageBean> packs = new Vector<>();
        packs.add(packageBean);*/

        //informations.checkMethodsThatCauseLackOfCohesion(packs);
        ArrayList<MethodBean> infectedMethodList = informations.getMethodsThatCauseLackOfCohesion();

        if (infectedMethodList.size() > 0) {

            for (MethodBean infect : infectedMethodList) {
                System.out.println(infect.getName());
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
        Collection<MethodBean> allMethods = informations.getTestClass().getMethods();
        MethodBean setup = null;
        for (MethodBean metodo : allMethods) {
            PsiMethod metodo2 = PsiUtil.getPsi(metodo, project, psiOriginalClass);

            if (!methodsToMove.contains(metodo2) && infectedMethodList.contains(metodo2)) {

                if (!metodo.getName().equals("setUp") && !methodsToMove.contains(metodo2)) {
                    methodsToMove.add(metodo2);
                } else if (metodo.getName().equals("setUp")) {
                    setup = metodo;
                }
            }
        }
        if(setup != null){
            PsiMethod psiSetup = PsiUtil.getPsi(setup, project, psiOriginalClass);

            ArrayList<PsiElement> elementsToMoveTemp = new ArrayList<>();

            for (MethodBean metodo : allMethods) {

                Vector<InstanceVariableBean> instances = (Vector<InstanceVariableBean>) metodo.getUsedInstanceVariables();

                for (int j = 0; j < psiSetup.getBody().getStatements().length; j++) {
                    PsiElement statement = psiSetup.getBody().getStatements()[j].getFirstChild();

                    if (statement instanceof PsiAssignmentExpression) {
                        PsiElement element = ((PsiAssignmentExpression) statement).getLExpression();

                        for (int i = 0; i < instances.size(); i++) {

                            if (element.getText().equals(instances.get(i).getName())) {
                                elementsToMoveTemp.add(element);
                            }
                        }
                    }
                }
            }
            PsiElement[] elementsToMove = elementsToMoveTemp.toArray(new PsiElement[elementsToMoveTemp.size()]);
            editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            ExtractMethodProcessor methodProcessor = new ExtractMethodProcessor(project, editor, elementsToMove, null, "setUpRefactored", "setUp", null);

            if (methodProcessor.prepare()) {
                methodProcessor.testPrepare();
                methodProcessor.testNullability();

                ExtractMethodHandler.extractMethod(project, methodProcessor);
                for (PsiClass innerClass : psiOriginalClass.getInnerClasses()) {
                    innerClasses.add(innerClass);
                }
                //Creo l'annotation "Before" da aggiungere al metodo appena estratto
                PsiAnnotation annotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText("@Before", methodProcessor.getExtractedMethod().getContext());

                //Aggiungo l'annotation creata al metodo
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    psiOriginalClass.addBefore(annotation, methodProcessor.getExtractedMethod());
                });

                methodsToMove.add(methodProcessor.getExtractedMethod());

                //Utile per cancellare la chiamata al nuovo metodo creato
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    PsiStatement[] statements = psiSetup.getBody().getStatements();
                    for (PsiStatement statement : statements) {
                        System.out.println(statement.getText());
                        if (statement.getText().equals("setUp();")) {
                            statement.delete();
                        }
                    }
                });
            }
            String classShortName = "Refactored" + originalClass.getName();

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
}
