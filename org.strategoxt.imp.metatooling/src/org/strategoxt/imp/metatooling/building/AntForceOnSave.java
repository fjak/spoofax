package org.strategoxt.imp.metatooling.building;

import java.io.FileNotFoundException;

import org.eclipse.core.runtime.Path;
import org.eclipse.imp.model.ModelFactory.ModelException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.FileState;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.services.OnSaveService;

public class AntForceOnSave {
    public static void main(String[] args) {
    	for(String arg : args) {
			try {
				FileState fileState = FileState.getFile(new Path(arg), null);
	    		IStrategoTerm ast = fileState.getAnalyzedAst();
	    		OnSaveService onSave = fileState.getDescriptor().createService(OnSaveService.class, fileState.getParseController());
	    		onSave.invokeOnSave(ast);
			} catch (FileNotFoundException e) {
				Environment.logException("Could not call on-save handler.", e);
			} catch (BadDescriptorException e) {
				Environment.logException("Could not call on-save handler.", e);
			} catch (ModelException e) {
				Environment.logException("Could not call on-save handler.", e);
			}
    	}
    }
}
