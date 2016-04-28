package org.walkmod.checkstyle.treewalkers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.checkstyle.visitors.AbstractCheckStyleRule;
import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.ImportDeclaration;
import org.walkmod.javalang.ast.SymbolData;
import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.ast.SymbolReference;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.compiler.symbols.RequiresSemanticAnalysis;

@RequiresSemanticAnalysis
public class AvoidStarImport<A> extends AbstractCheckStyleRule<A> {

   @Override
   public void visit(ImportDeclaration node, A ctx) {
      if (node.isAsterisk()) {
         List<SymbolReference> refs = node.getUsages();
         Map<String, List<SymbolReference>> classes = new HashMap<String, List<SymbolReference>>();
         if (refs != null) {
            for (SymbolReference sr : refs) {
               if (sr instanceof SymbolDataAware<?>) {
                  SymbolDataAware<?> aux = (SymbolDataAware<?>) sr;
                  SymbolData sd = aux.getSymbolData();
                  if (sd != null) {
                     List<SymbolReference> refsAux = null;
                     if (classes.containsKey(sd.getName())) {
                        refsAux = classes.get(sd.getName());
                     } else {
                        refsAux = new LinkedList<SymbolReference>();
                        classes.put(sd.getName(), refsAux);
                     }
                     refsAux.add(sr);

                  }
               }
            }
         }
         CompilationUnit cu = (CompilationUnit) node.getParentNode();
         List<ImportDeclaration> imports = new LinkedList<ImportDeclaration>(cu.getImports());
         Iterator<ImportDeclaration> it = imports.iterator();
         int i = -1;
         boolean found = false;
         while (it.hasNext() && !found) {
            i++;
            ImportDeclaration current = it.next();
            found = (current == node);
         }
         if (found) {
            it.remove();
            if (!classes.isEmpty()) {
               for (String clazz : classes.keySet()) {
                  ImportDeclaration id;
                  try {
                     id = new ImportDeclaration((NameExpr) ASTManager.parse(NameExpr.class, clazz), node.isStatic(),
                           false);
                     List<SymbolReference> refsAux = classes.get(clazz);
                     for(SymbolReference sr: refsAux){
                        sr.setSymbolDefinition(id);
                     }
                     id.setUsages(refsAux);
                     imports.add(i, id);
                  } catch (ParseException e) {
                     throw new RuntimeException(e);
                  }
               }
            }
         }
         cu.setImports(imports);
      }
   }
}
