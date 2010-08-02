/*******************************************************************************
 * Copyright (c) 2010 by Timotei Dolean <timotei21@gmail.com>
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.wesnoth.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.LeafNode;
import org.eclipse.xtext.parsetree.NodeUtil;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.wesnoth.ui.WMLUiModule;

import wesnoth_eclipse_plugin.schema.SchemaParser;
import wesnoth_eclipse_plugin.schema.Tag;
import wesnoth_eclipse_plugin.schema.TagKey;

public class WMLProposalProvider extends AbstractWMLProposalProvider
{
	public WMLProposalProvider()
	{
		super();
		// load the schema so we know what to suggest for autocomplete
		SchemaParser.getInstance().parseSchema(false);
	}

	@Override
	public void completeWMLTag_Name(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor)
	{
		super.completeWMLTag_Name(model, assignment, context, acceptor);
		dbg("completing wmltagname");
		if (context.getCurrentNode().eContainer() != null &&
			context.getCurrentNode().eContainer() instanceof AbstractNode)
		{
			AbstractNode node = (AbstractNode)context.getCurrentNode().eContainer();

			if (node.getParent().eContainer() != null) // we are not at the root
			{
				LeafNode parent = (LeafNode)NodeUtil.findLeafNodeAtOffset(node.getParent(),
						node.getParent().getOffset() + 2);
				String parentIndent = ((LeafNode)NodeUtil.findLeafNodeAtOffset(node.getParent(),
						node.getOffset())).getText();
//				dbg("parent text: " + parent.getText());
				parentIndent =  parentIndent.replace("\r", "").replace("\n", "");
				Tag tagChildren = SchemaParser.getInstance().getTags().get(parent.getText());
				if (tagChildren != null)
				{
					for(Tag tag : tagChildren.getTagChildren())
					{
						acceptor.accept(tagProposal(tag, parentIndent, context));
					}
				}
				else
					dbg("!!! no tag found with that name:" + parent.getText());
			}
			else // we are at the root
			{
				Tag rootTag = SchemaParser.getInstance().getTags().get("root");
				dbg("root node. adding tags: "+ rootTag.getTagChildren().size());
				for(Tag tag : rootTag.getTagChildren())
				{
					acceptor.accept(tagProposal(tag, "", context));
				}
			}
		}
		else
			dbg("current container is null or not an abstract node.");
	}

	/**
	 * Returns the proposal for the specified tag, usign the specified indent
	 * @param tag The tag from which to construct the proposal
	 * @param indent The indent used to indent the tag and subsequent keys
	 * @return
	 */
	private ICompletionProposal tagProposal(Tag tag, String indent,
					ContentAssistContext context)
	{
//		dbg("indent:[" + indent +"]");
		StringBuilder proposal = new StringBuilder();
		proposal.append(tag.getName());
		proposal.append("]\n");
		for(TagKey key : tag.getKeyChildren())
		{
			if (key.getCardinality() == '1')
				proposal.append(String.format("\t%s%s=\n",
						indent, key.getName()));
		}
		proposal.append(String.format("%s[/%s]",indent, tag.getName()));
		return createCompletionProposal(tag.getName(), proposal.toString(), context);
	}

	private ICompletionProposal createCompletionProposal(String displayName, String content,
					ContentAssistContext context)
	{
		return createCompletionProposal(content, displayName, null, context);
	}

	/**
	 * Method for debugging the auto completion
	 * @param str
	 */
	private void dbg(Object str)
	{
		if (!(WMLUiModule.DEBUG))
			return;
		System.out.println(str.toString());
	}
}
