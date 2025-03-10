package io.onedev.server.entityreference;

import com.google.common.collect.ImmutableSet;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.*;
import io.onedev.server.util.HtmlUtils;
import io.onedev.server.util.ProjectScopedNumber;
import io.onedev.server.util.TextNodeVisitor;
import io.onedev.server.validation.validator.ProjectPathValidator;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReferenceParser {
	
	private static final Collection<String> IGNORED_TAGS = ImmutableSet.of("code", "a");
	
	private final Pattern pattern;
	
	public ReferenceParser(Class<? extends AbstractEntity> referenceClass) {
		StringBuilder builder = new StringBuilder();
		if (referenceClass == Build.class) 
			builder.append("(^|\\W+)((build)\\s+)(");
		else if (referenceClass == PullRequest.class) 
			builder.append("(^|\\W+)((pull\\s*request|pr)\\s+)(");
		else if (referenceClass == Issue.class) 
			builder.append("(^|\\W+)((issue)\\s+)?(");
		else 
			throw new RuntimeException("Unexpected reference class: " + referenceClass);
		
		builder.append(ProjectPathValidator.PATTERN.pattern());
		builder.append(")?#(\\d+)(?=$|[\\W|/]+)");
		pattern = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
	}
	
	public List<ProjectScopedNumber> parseReferences(String text, @Nullable Project project) {
		Collection<ProjectScopedNumber> references = new LinkedHashSet<>();
		var projectManager = OneDev.getInstance(ProjectManager.class);
		if (fastScan(text)) { 
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				try {
					String referenceProjectName = matcher.group(4);
					Long referenceNumber = Long.valueOf(matcher.group(6));
					Project referenceProject;
					if (referenceProjectName != null) {
						referenceProject = projectManager.findByPath(referenceProjectName);
					} else {
						referenceProject = project;
					}
					if (referenceProject != null)
						references.add(new ProjectScopedNumber(referenceProject, referenceNumber));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return new ArrayList<>(references);
	}
	
	public static boolean fastScan(String text) {
		var chars = text.toCharArray();
		for (var i=0; i<chars.length; i++) {
			if (chars[i] == '#' && i<chars.length-1 && Character.isDigit(chars[i+1]))
				return true;
		}
		return false;
	}
	
	public List<ProjectScopedNumber> parseReferences(Document document, @Nullable Project project) {
		Collection<ProjectScopedNumber> references = new LinkedHashSet<>();
		
		TextNodeVisitor visitor = new TextNodeVisitor() {
			
			@Override
			protected boolean isApplicable(TextNode node) {
				if (HtmlUtils.hasAncestor(node, IGNORED_TAGS))
					return false;
				
				return fastScan(node.getWholeText());
			}
		};
		
		NodeTraversor.traverse(visitor, document);
		
		var projectManager = OneDev.getInstance(ProjectManager.class);
		for (TextNode node : visitor.getMatchedNodes()) {
			Matcher matcher = pattern.matcher(node.getWholeText());
			while (matcher.find()) {
				String referenceText = matcher.group(2);
				if (referenceText == null)
					referenceText = "";
				String referenceProjectName = matcher.group(4);
				Long referenceNumber = Long.valueOf(matcher.group(6));

				Project referenceProject;
				if (referenceProjectName != null) {
					referenceProject = projectManager.findByPath(referenceProjectName);
					referenceText += referenceProjectName;
				} else {
					referenceProject = project;
				}
				referenceText += "#" + referenceNumber;
				
				String referenceTag;
				if (referenceProject != null) {
					ProjectScopedNumber referenceable = new ProjectScopedNumber(referenceProject, referenceNumber);
					references.add(referenceable);
					referenceTag = toHtml(referenceable, referenceText);
				} else {
					referenceTag = referenceText; 
				}
				HtmlUtils.appendReplacement(matcher, node, matcher.group(1) + referenceTag);
			}
			HtmlUtils.appendTail(matcher, node);
		}

		return new ArrayList<>(references);
	}

	protected String toHtml(ProjectScopedNumber referenceable, String referenceText) {
		return referenceText;
	}
	
}
