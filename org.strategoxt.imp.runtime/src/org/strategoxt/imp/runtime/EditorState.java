package org.strategoxt.imp.runtime;

import lpg.runtime.IPrsStream;
import lpg.runtime.IToken;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.imp.runtime.parser.ast.AstNode;
import org.strategoxt.imp.runtime.parser.tokens.SGLRToken;
import org.strategoxt.imp.runtime.parser.tokens.TokenKind;
import org.strategoxt.imp.runtime.stratego.StrategoTermPath;
import org.strategoxt.imp.runtime.stratego.adapter.IStrategoAstNode;

/**
 * Helper class for accessing the active editor.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class EditorState {
	
	private final UniversalEditor editor;
	
	public EditorState(UniversalEditor editor) {
		this.editor = editor;
	}
	
	// FACTORY METHODS
	
	/**
	 * Gets the editor state for the active editor.
	 * 
	 * @throws IllegalStateException  if not called from the UI thread.
	 */
	public static EditorState getActiveEditor() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			throw new IllegalStateException("Must be called from UI thread");
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null)
			return null;
		IEditorPart editor = activePage.getActiveEditor();
		return editor instanceof UniversalEditor && ((UniversalEditor) editor).getParseController() instanceof DynamicParseController
			? new EditorState((UniversalEditor) editor)
			: null;
	}
	
	/**
	 * Returns the editor state associated with a given parse controller,
	 * if any active editor can be found.
	 * 
	 * @throws IllegalStateException  if not called from the UI thread
	 */
	public static EditorState getEditorFor(IParseController parseController) {
		if (parseController instanceof SGLRParseController)
			return ((SGLRParseController) parseController).getEditor();
		
		EditorState activeEditor = getActiveEditor();
		if (activeEditor != null && activeEditor.getEditor().getParseController() == parseController)
			return activeEditor;
		
		// Search for another editor with this parser
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference reference : page.getEditorReferences()) {
					IWorkbenchPart editor = reference.getPart(false);
					if (editor instanceof UniversalEditor
							&& ((UniversalEditor) editor).getParseController() == parseController) {
						// HACK: showChangeInformation(false)
						((UniversalEditor) editor).showChangeInformation(false);
						return new EditorState((UniversalEditor) editor);
					}
				}
			}
		}
		
		return null;
	}
	
	// ACCESSORS
	
	public static boolean isUIThread() {
		// return Display.getCurrent() != null; // may exist in multiple threads
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null;
	}

	public UniversalEditor getEditor() {
		return editor;
	}
	
	public SGLRParseController getParseController() {
		DynamicParseController wrapper = (DynamicParseController) getEditor().getParseController();
		return (SGLRParseController) wrapper.getWrapped();
	}
	
	public Language getLanguage() {
		return getEditor().fLanguage;
	}
	
	public final Descriptor getDescriptor() {
		return Environment.getDescriptor(getLanguage());
	}

	public final IResource getResource() {
    	return getParseController().getResource();
	}
	
	public final ISourceProject getProject() {
		return getParseController().getProject();
	}
	
	/**
	 * @see SGLRParseController#getCurrentAst
	 */
	public final AstNode getCurrentAst() {
		return getParseController().getCurrentAst();
	}
	
	public void scheduleParserUpdate(long delay) {
		if (!getEditor().getTitleImage().isDisposed())
			getEditor().fParserScheduler.schedule(delay);
	}
	
	/**
	 * Gets the document model for this editor, which can be used to manipulate
	 * the contents of the editor.
	 * 
	 * See http://wiki.eclipse.org/FAQ_How_do_I_insert_text_in_the_active_text_editor%3F
	 */
	public IDocument getDocument() {
		IDocumentProvider provider = getEditor().getDocumentProvider();
		return provider.getDocument(getEditor().getEditorInput());
	}
	
	/**
	 * Gets the abstract syntax subtree for the selection in the editor.
	 * 
	 * @param ignoreEmptyEmptySelection
	 *            If true, null is returned if the selection is 0 characters wide.
	 * 
	 * @see SGLRParseController#getCurrentAst()
	 *            Gets the entire AST.
	 */
	public final synchronized IStrategoAstNode getSelectionAst(boolean ignoreEmptyEmptySelection) {
		Point selection = getEditor().getSelection();
		if (ignoreEmptyEmptySelection && selection.y == 0)
			return null;
		
		IToken start = getParseController().getTokenIterator(new Region(selection.x, 0), true).next();
		IToken end = getParseController().getTokenIterator(new Region(selection.x + selection.y - 1, 0), true).next();
		
		IPrsStream tokens = start.getIPrsStream();
		int layout = TokenKind.TK_LAYOUT.ordinal();
		int eof = TokenKind.TK_EOF.ordinal();
		
		while (start.getKind() == layout && start.getTokenIndex() < tokens.getSize())
			start = tokens.getTokenAt(start.getTokenIndex() + 1);
		
		while ((end.getKind() == layout || end.getKind() == eof) && end.getTokenIndex() > 0)
			end = tokens.getTokenAt(end.getTokenIndex() - 1);
		
		IStrategoAstNode startNode = ((SGLRToken) start).getAstNode();
		IStrategoAstNode endNode = ((SGLRToken) end).getAstNode();

		return StrategoTermPath.findCommonAncestor(startNode, endNode);
	}

	/**
	 * Asynchronously opens or activates an editor.
	 * 
	 * Exceptions are swallowed and logged.
	 */
	public static void asyncOpenEditor(Display display, final IFile file, final boolean activate) {
		display.asyncExec(new Runnable() {
			public void run() {
				openEditor(file, activate);
			}
		});
	}

	/**
	 * Opens a new editor. Must be invoked from the UI thread.
	 * 
	 * PartInitExceptions are swallowed and logged.
	 */
	public static void openEditor(final IFile file, final boolean activate) {
		if (!isUIThread())
			throw new IllegalStateException("Must be called from UI thread");
		
		IWorkbenchPage page =
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IDE.openEditor(page, file, UniversalEditor.EDITOR_ID, activate);
		} catch (PartInitException e) {
			Environment.logException("Cannot open an editor for " + file, e);
		}
	}
}