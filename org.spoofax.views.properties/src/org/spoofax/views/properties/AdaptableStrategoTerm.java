package org.spoofax.views.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.spoofax.interpreter.terms.IStrategoTerm;

// TODO: move this class to package org.spoofax.interpreter.terms?
// TODO: make IStrategoTerm adaptable instead?
public class AdaptableStrategoTerm implements IAdaptable {

	private IStrategoTerm term;

	public AdaptableStrategoTerm(IStrategoTerm term) {
		this.term = term;
	}
	
    /**
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */  
	@Override
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
	
	public IStrategoTerm getTerm() {
		return term;
	}
}
