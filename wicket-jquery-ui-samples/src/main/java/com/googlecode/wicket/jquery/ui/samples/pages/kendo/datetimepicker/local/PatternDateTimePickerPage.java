package com.googlecode.wicket.jquery.ui.samples.pages.kendo.datetimepicker.local;

import java.time.LocalDateTime;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import com.googlecode.wicket.kendo.ui.form.button.AjaxButton;
import com.googlecode.wicket.kendo.ui.form.button.Button;
import com.googlecode.wicket.kendo.ui.form.datetime.local.DateTimePicker;
import com.googlecode.wicket.kendo.ui.panel.KendoFeedbackPanel;

public class PatternDateTimePickerPage extends AbstractTimePickerPage
{
	private static final long serialVersionUID = 1L;

	public PatternDateTimePickerPage()
	{
		Form<?> form = new Form<Void>("form");
		this.add(form);

		// FeedbackPanel //
		final KendoFeedbackPanel feedback = new KendoFeedbackPanel("feedback");
		form.add(feedback);

		// DateTimePicker //
		final DateTimePicker datetimepicker = new DateTimePicker("datetimepicker", Model.of(LocalDateTime.now()), "EEE dd MMM yyyy", "HH:mm:ss");
		form.add(datetimepicker);

		// Buttons //
		form.add(new Button("submit") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit()
			{
				this.info("Date & Time: " + datetimepicker.getModelObject());
			}
		});

		form.add(new AjaxButton("button") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form)
			{
				this.info("Date & Time: " + datetimepicker.getModelObject());
				target.add(feedback);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form)
			{
				target.add(feedback);
			}
		});
	}
}
