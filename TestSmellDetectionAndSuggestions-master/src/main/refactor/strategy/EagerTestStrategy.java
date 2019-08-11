package main.refactor.strategy;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ChangeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import it.unisa.testSmellDiffusion.beans.ClassBean;
import it.unisa.testSmellDiffusion.beans.MethodBean;
import it.unisa.testSmellDiffusion.testSmellInfo.eagerTest.EagerTestInfo;
import it.unisa.testSmellDiffusion.testSmellInfo.eagerTest.MethodWithEagerTest;
import main.refactor.IRefactor;
import com.intellij.openapi.project.Project;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

public class EagerTestStrategy implements IRefactor {
    private EagerTestInfo eagerTestInfo;
    private MethodWithEagerTest methodWithEagerTest;
    private Project project;
    private Editor editor;

    public EagerTestStrategy(MethodWithEagerTest methodWithEagerTest, Project project, EagerTestInfo eagerTestInfo) {
        this.methodWithEagerTest = methodWithEagerTest;
        this.project = project;
        this.eagerTestInfo = eagerTestInfo;
    }

    @Override
    public void doRefactor() throws PrepareFailedException {
        ClassBean badClass = eagerTestInfo.getTestClass();
        PsiClass classPsi = PsiUtil.getPsi(badClass, project);
        String initialMethodName = methodWithEagerTest.getMethod().getName(); //nome del metodo infetto
        boolean isEmpty = true;

        ArrayList<MethodBean> calledMethods = methodWithEagerTest.getListOfMethodsCalled(); //non ci sono tutti i metodi chiamati

        MethodBean badMethod = methodWithEagerTest.getMethod();
        PsiMethod psiMethod = PsiUtil.getPsi(badMethod, project, classPsi);
        PsiType type = psiMethod.getReturnType();

        ArrayList<PsiElement> elementsToMoveTemp = new ArrayList<>();
        //PsiElement[] elementsToMoveTemp = new PsiElement[1];
        for (int i = 0; i < classPsi.getAllMethods().length; i++) {

            if (classPsi.getAllMethods()[i].getName().equals(initialMethodName)) {

                for (int j = 0; j < classPsi.getAllMethods()[i].getBody().getStatements().length; j++) {

                    PsiElement statement = classPsi.getAllMethods()[i].getBody().getStatements()[j].getFirstChild();

                    if (statement instanceof PsiMethodCallExpression && statement.getFirstChild() instanceof PsiReferenceExpression) {
                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) statement.getFirstChild();
                        String identifier = referenceExpression.getReferenceName();

                        if (isEmpty) {
                            //elementsToMoveTemp[0] = classPsi.getAllMethods()[i].getBody().getStatements()[j];
                            elementsToMoveTemp.add(classPsi.getAllMethods()[i].getBody().getStatements()[j]);
                            isEmpty = false;
                        } else {

                            PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) elementsToMoveTemp.get(0).getFirstChild().getFirstChild();
                            String referenceName = psiReferenceExpression.getReferenceName();

                            if (identifier.equals(referenceName)) {
                                elementsToMoveTemp.add(classPsi.getAllMethods()[i].getBody().getStatements()[j]);
                            }
                        }
                    }
                }
            }
        }
        PsiElement[] elementsToMove = elementsToMoveTemp.toArray( new PsiElement[elementsToMoveTemp.size()]);
        editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        ExtractMethodProcessor processor = new ExtractMethodProcessor(project, editor, elementsToMove, type, "newOne", "refactoredOne", null);

        if (processor.prepare()) {
            processor.testPrepare();
            processor.testNullability();
            ExtractMethodHandler.extractMethod(project, processor);
        }

        //Creo l'annotation "Before" da aggiungere al metodo appena estratto
        PsiAnnotation annotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText("@Test",processor.getExtractedMethod().getContext());

        //Aggiungo l'annotation creata al metodo
        WriteCommandAction.runWriteCommandAction(project, () -> {
            classPsi.addBefore(annotation, processor.getExtractedMethod());
        });

        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiStatement[] statements = psiMethod.getBody().getStatements();
            for(PsiStatement statement : statements){
                if(statement.getText().equals("refactoredOne();")){
                    statement.delete();
                }
            }
        });
    }
}
