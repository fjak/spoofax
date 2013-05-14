package org.spoofax.views.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.spoofax.interpreter.terms.IStrategoTerm;

// TODO: move this class somewhere else, because it's not specific to the properties view 
//       (probably also needed for the outline view)
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
