package org.strategoxt.imp.runtime.services;

import static org.strategoxt.imp.runtime.parser.tokens.TokenKind.*;

import java.util.List;
import java.util.Stack;

import lpg.runtime.IPrsStream;
import lpg.runtime.IToken;

import org.eclipse.imp.parser.IParseController;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.parser.ast.AbstractVisitor;
import org.strategoxt.imp.runtime.parser.ast.AstNode;

/**
 * Base class for an outliner.
 * 
 * @author Lennart Kats <L.C.L.Kats add tudelft.nl>
 */
public class Outliner extends org.eclipse.imp.services.base.OutlinerBase {
	// TODO: Re-implement outline based on TreeModelBuilderBase 
	
	/*
	 * A shadow copy of the outline stack maintained in the super class,
	 * holding all pushed AST nodes (rather than outline items).
	 * 
	 * (When not using a strongly typed visitor, this
	 *  should be the most efficient and simple way of determining
	 *  whether an outline item should be popped.)
	 */
	private Stack<AstNode> outlineStack = new Stack<AstNode>();
	
	private final List<NodeMapping> rules;
	
	public Outliner(List<NodeMapping> rules) {
		this.rules = rules;
	}	
	
    private class OutlineVisitor extends AbstractVisitor {
		public boolean preVisit(AstNode node) {
			if (NodeMapping.hasAttribute(rules, node.getConstructor(), node.getSort(), 0))
				outline(node);
			
			return true;
		}

		public void postVisit(AstNode node) {
			endOutline(node);
		}
	}
	
	// Interface implementation
	
	@Override
	protected final void sendVisitorToAST(Object node) {
		((AstNode) node).accept(new OutlineVisitor());
	}
	
	@Override
	protected boolean significantChange(IParseController controller) {
		return true; // HACK: parent method is now LPG specific
	}
	
	// Helper methods
	
	// These help select an outline item's caption,
	// in case the outline descriptor didn't include a pattern
	// to find it.
	
	protected void outline(AstNode node) {
		String caption = getIdentifierHelper(node);
		
		if (caption != null) {
			outline(node, caption);
		} else {
			Environment.logException(
				"Unable to infer the caption of this AST node: " +
				node.getSort() + "." + node.getConstructor()
			);
		}
	}

	private String getIdentifierHelper(AstNode node) {
		// HACK: Hardcoded outlining, until we have support for patterns
		String constructor = node == null ? null : node.getConstructor();
		
		if ("MethodDec".equals(constructor)) {
			return node.getChildren().get(0).getChildren().get(3).toString();
		} else if ("ClassDec".equals(constructor)) {
			return node.getChildren().get(0).getChildren().get(1).toString();
		} else if ("Rules".equals(constructor)) {
			return "rules";
		} else if ("Strategies".equals(constructor)) {
			return "strategies";
		} else {
			return getIdentifier(node);
		}
	}
	
	protected void outline(AstNode node, String caption) {
		if (outlineStack.isEmpty()) {
			pushTopItem(caption, node);
		} else {
			pushSubItem(caption, node);
		}
		
		outlineStack.push(node);
	}
	
	protected void endOutline(AstNode node) {
		if (!outlineStack.isEmpty() && outlineStack.peek() == node) {
			if (outlineStack.size() > 1) // don't pop top items
				popSubItem();
			
			outlineStack.pop();
		}
	}
	
	// TODO2: Optimize - cache getIdentifier?
	
	// TODO: Don't return identifier tokens in list or optional AST nodes
	
	private String getIdentifier(AstNode node) {
		IPrsStream stream = node.getLeftIToken().getIPrsStream();
		int i = node.getLeftIToken().getTokenIndex();
		int end = node.getRightIToken().getTokenIndex();
		
		do {
			IToken token = stream.getTokenAt(i);
			int kind = token.getKind();

			if (kind == TK_IDENTIFIER.ordinal() || kind == TK_STRING.ordinal())
				return token.toString();
			
		} while (i++ < end);
		
		return null;
	}
}