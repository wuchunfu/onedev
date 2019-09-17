package io.onedev.server.web.editable.buildchoice;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.google.common.base.Preconditions;

import io.onedev.commons.utils.ReflectionUtils;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.util.OneContext;
import io.onedev.server.web.component.build.choice.BuildChoiceProvider;
import io.onedev.server.web.component.build.choice.BuildSingleChoice;
import io.onedev.server.web.editable.ErrorContext;
import io.onedev.server.web.editable.PathElement;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.annotation.BuildChoice;
import io.onedev.server.web.util.ProjectAware;

@SuppressWarnings("serial")
public class BuildChoiceEditor extends PropertyEditor<Long> {

	private BuildSingleChoice input;
	
	public BuildChoiceEditor(String id, PropertyDescriptor propertyDescriptor, 
			IModel<Long> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Nullable
	private Project getProject() {
		OneContext.push(new OneContext(this));
		try {
			BuildChoice choice = Preconditions.checkNotNull(descriptor
					.getPropertyGetter().getAnnotation(BuildChoice.class));
			if (choice.value().length() != 0) {
				return (Project) ReflectionUtils.invokeStaticMethod(
						descriptor.getBeanClass(), choice.value());
			} else {
				return findParent(ProjectAware.class).getProject();
			}
		} finally {
			OneContext.pop();
		}
	}
	
	@Nullable
	private Build getBuild() {
		if (getProject() != null && getModelObject() != null)
			return OneDev.getInstance(BuildManager.class).find(getProject(), getModelObject());
		else
			return null;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		Build build;
		if (getProject() != null && getModelObject() != null)
			build = OneDev.getInstance(BuildManager.class).find(getProject(), getModelObject());
		else
			build = null;
		
		BuildChoiceProvider choiceProvider = new BuildChoiceProvider(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return getProject();
			}
    		
    	});
    	input = new BuildSingleChoice("input", Model.of(build), choiceProvider) {

    		@Override
			protected void onInitialize() {
				super.onInitialize();
				getSettings().configurePlaceholder(descriptor, this);
			}
    		
    	};
        input.setConvertEmptyInputStringToNull(true);
        
        // add this to control allowClear flag of select2
    	input.setRequired(descriptor.isPropertyRequired());
        input.setLabel(Model.of(getDescriptor().getDisplayName(this)));
        
		input.add(new AjaxFormComponentUpdatingBehavior("change"){

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				onPropertyUpdating(target);
			}
			
		});
		add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathElement element) {
		return null;
	}

	@Override
	protected Long convertInputToValue() throws ConversionException {
		Build build = input.getConvertedInput();
		if (build != null)
			return build.getNumber();
		else
			return null;
	}

}
