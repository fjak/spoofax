package org.spoofax.views.properties;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

public class StrategoTermAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IPropertySource.class) {
			return new StrategoTermPropertySource(((AdaptableStrategoTerm) adaptableObject).getTerm());
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
