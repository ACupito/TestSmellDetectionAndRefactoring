package main.refactor.strategy;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import it.unisa.testSmellDiffusion.beans.ClassBean;
import it.unisa.testSmellDiffusion.beans.MethodBean;
import it.unisa.testSmellDiffusion.testSmellInfo.eagerTest.EagerTestInfo;
import it.unisa.testSmellDiffusion.testSmellInfo.eagerTest.MethodWithEagerTest;
import main.refactor.IRefactor;
import com.intellij.openapi.project.Project;
import main.refactor.manipulator.MethodManipulator;
import sun.java2d.loops.DrawGlyphListAA;
import sun.java2d.loops.FillRect;

import java.util.ArrayList;

public class EagerTestStrategy implements IRefactor {
    private EagerTestInfo eagerTestInfo;
    private MethodWithEagerTest methodWithEagerTest;
    private Project project;

    public EagerTestStrategy(MethodWithEagerTest methodWithEagerTest, Project project, EagerTestInfo eagerTestInfo){
        this.methodWithEagerTest = methodWithEagerTest;
        this.project = project;
        this.eagerTestInfo = eagerTestInfo;
    }

    @Override
    public void doRefactor() {
        ClassBean badClass = eagerTestInfo.getTestClass();
        PsiClass classPsi = PsiUtil.getPsi(badClass, project);
        String initialMethodName = methodWithEagerTest.getMethod().getName(); //nome del metodo infetto

        ArrayList<MethodBean> calledMethods =  methodWithEagerTest.getListOfMethodsCalled(); //non ci sono tutti i metodi chiamati
        PsiElement[] elementsToMove = new PsiElement[calledMethods.size()];
        int k=0;

        for(int i=0; i < classPsi.getAllMethods().length; i++){

            if(classPsi.getAllMethods()[i].getName().toString().equals(initialMethodName)){

                for(int j=0; j < classPsi.getAllMethods()[i].getBody().getStatements().length; j++){

                    PsiElement statement = classPsi.getAllMethods()[i].getBody().getStatements()[j].getFirstChild();

                    if(statement instanceof PsiMethodCallExpression && statement.getFirstChild() instanceof PsiReferenceExpression){
                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) statement.getFirstChild();
                        PsiIdentifier identifier = (PsiIdentifier) referenceExpression.getReference();

                        for(int z=0; z < calledMethods.size(); z++) {

                            if (identifier.getText().equals(calledMethods.get(z).getName())){
                                elementsToMove[k++] = classPsi.getAllMethods()[i].getBody().getStatements()[j];
                            }
                        }
                    }
                }
            }
        }
        //ExtractMethodProcessor processor = new ExtractMethodProcessor(project, );
        /*for(MethodBean method : calledMethods){

            /*String scope = "public";
            String returnType = method.getReturnType().toString();
            String nome = method.getName() + "Test";
            String parameters = "(" + method.getParameters().toString() + ")";
            String body = method.getTextContent();

            String newMethod = MethodManipulator.buildMethod(body);
            MethodManipulator.methodWriter(newMethod,classPsi, project);
        }*/
    }
}
