/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.wicket.jquery.ui.calendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.util.string.Strings;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.JQueryEvent;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.ajax.IJQueryAjaxAware;
import com.googlecode.wicket.jquery.core.ajax.JQueryAjaxBehavior;
import com.googlecode.wicket.jquery.core.utils.RequestCycleUtils;
import com.googlecode.wicket.jquery.ui.calendar.settings.CalendarLibrarySettings;

/**
 * Provides the jQuery fullCalendar behavior
 *
 * @author Sebastien Briquet - sebfz1
 *
 */
public abstract class CalendarBehavior extends JQueryBehavior implements IJQueryAjaxAware, ICalendarListener
{
	private static final long serialVersionUID = 1L;
	public static final String METHOD = "fullCalendar";

	/** date range-select behavior */
	private JQueryAjaxBehavior onSelectAjaxBehavior = null;

	/** day click */
	private JQueryAjaxBehavior onDayClickAjaxBehavior;

	/** event click */
	private JQueryAjaxBehavior onEventClickAjaxBehavior;

	/** event drop */
	private JQueryAjaxBehavior onEventDropAjaxBehavior = null;

	/** event resize */
	private JQueryAjaxBehavior onEventResizeAjaxBehavior = null;

	/** event-object drop */
	private JQueryAjaxBehavior onObjectDropAjaxBehavior = null;

	/** view render */
	private JQueryAjaxBehavior onViewRenderAjaxBehavior = null;

	/**
	 * Constructor
	 *
	 * @param selector the html selector (ie: "#myId")
	 */
	public CalendarBehavior(final String selector)
	{
		this(selector, new Options());
	}

	/**
	 * Constructor
	 *
	 * @param selector the html selector (ie: "#myId")
	 * @param options the {@link Options}
	 */
	public CalendarBehavior(final String selector, Options options)
	{
		super(selector, METHOD, options);

		this.initReferences();
	}

	/**
	 * Initializes CSS & JavaScript resource references
	 */
	private void initReferences()
	{
		CalendarLibrarySettings settings = CalendarLibrarySettings.get();

		// fullcalendar.css //
		if (settings.getStyleSheetReference() != null)
		{
			this.add(settings.getStyleSheetReference());
		}

		// fullcalendar.min.js //
		if (settings.getJavaScriptReference() != null)
		{
			this.add(settings.getJavaScriptReference());
		}

		// gcal.js //
		if (settings.getGCalJavaScriptReference() != null)
		{
			this.add(settings.getGCalJavaScriptReference());
		}
	}

	// Methods //

	@Override
	public void bind(Component component)
	{
		super.bind(component);

		if (this.isSelectable())
		{
			this.onSelectAjaxBehavior = this.newOnSelectAjaxBehavior(this);
			component.add(this.onSelectAjaxBehavior);
		}

		if (this.isDayClickEnabled())
		{
			this.onDayClickAjaxBehavior = this.newOnDayClickAjaxBehavior(this);
			component.add(this.onDayClickAjaxBehavior);
		}

		if (this.isEventClickEnabled())
		{
			this.onEventClickAjaxBehavior = this.newOnEventClickAjaxBehavior(this);
			component.add(this.onEventClickAjaxBehavior);
		}

		if (this.isEventDropEnabled())
		{
			this.onEventDropAjaxBehavior = this.newOnEventDropAjaxBehavior(this, this.getEventDropPrecondition());
			component.add(this.onEventDropAjaxBehavior);
		}

		if (this.isEventResizeEnabled())
		{
			this.onEventResizeAjaxBehavior = this.newOnEventResizeAjaxBehavior(this, this.getEventResizePrecondition());
			component.add(this.onEventResizeAjaxBehavior);
		}

		if (this.isObjectDropEnabled())
		{
			this.onObjectDropAjaxBehavior = this.newOnObjectDropAjaxBehavior(this);
			component.add(this.onObjectDropAjaxBehavior);
		}

		if (this.isViewRenderEnabled())
		{
			this.onViewRenderAjaxBehavior = this.newOnViewRenderAjaxBehavior(this);
			component.add(this.onViewRenderAjaxBehavior);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);

		IRequestHandler handler = new ResourceReferenceRequestHandler(AbstractDefaultAjaxBehavior.INDICATOR);

		/* adds and configure the busy indicator */
		StringBuilder builder = new StringBuilder();

		builder.append("jQuery(function(){\n");
		builder.append("jQuery(\"<img id='calendar-indicator' src='").append(RequestCycle.get().urlFor(handler)).append("' />\").appendTo('.fc-header-center');\n"); // allows only one calendar.
		builder.append("jQuery(document).ajaxStart(function() { jQuery('#calendar-indicator').show(); });\n");
		builder.append("jQuery(document).ajaxStop(function() { jQuery('#calendar-indicator').hide(); });\n");
		builder.append("});\n");

		response.render(JavaScriptHeaderItem.forScript(builder, this.getToken() + "-indicator"));
	}

	// Properties //

	/**
	 * Indicates whether the Calendar will be editable
	 *
	 * @return by default, true if {@link #isDayClickEnabled()} is true or {@link #isEventClickEnabled()} is true
	 */
	protected boolean isEditable()
	{
		return (this.onDayClickAjaxBehavior != null) || (this.onEventClickAjaxBehavior != null);
	}

	// Events //

	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);

		this.setOption("editable", this.isEditable());
		this.setOption("selectable", this.isSelectable());
		this.setOption("selectHelper", this.isSelectable());
		this.setOption("disableDragging", !this.isEventDropEnabled());
		this.setOption("disableResizing", !this.isEventResizeEnabled());
		this.setOption("droppable", this.isObjectDropEnabled());

		if (this.onSelectAjaxBehavior != null)
		{
			this.setOption("select", this.onSelectAjaxBehavior.getCallbackFunction());
		}

		if (this.onDayClickAjaxBehavior != null)
		{
			this.setOption("dayClick", this.onDayClickAjaxBehavior.getCallbackFunction());
		}

		if (this.onEventClickAjaxBehavior != null)
		{
			this.setOption("eventClick", this.onEventClickAjaxBehavior.getCallbackFunction());
		}

		if (this.onEventDropAjaxBehavior != null)
		{
			this.setOption("eventDrop", this.onEventDropAjaxBehavior.getCallbackFunction());
		}

		if (this.onEventResizeAjaxBehavior != null)
		{
			this.setOption("eventResize", this.onEventResizeAjaxBehavior.getCallbackFunction());
		}

		if (this.onObjectDropAjaxBehavior != null)
		{
			this.setOption("drop", this.onObjectDropAjaxBehavior.getCallbackFunction());
		}

		if (this.onViewRenderAjaxBehavior != null)
		{
			this.setOption("viewRender", this.onViewRenderAjaxBehavior.getCallbackFunction());
		}
	}

	@Override
	public void onAjax(AjaxRequestTarget target, JQueryEvent event)
	{
		if (event instanceof SelectEvent)
		{
			SelectEvent selectEvent = (SelectEvent) event;
			this.onSelect(target, selectEvent.getView(), selectEvent.getStart(), selectEvent.getEnd(), selectEvent.isAllDay());
		}

		else if (event instanceof DayClickEvent)
		{
			DayClickEvent dayClickEvent = (DayClickEvent) event;
			this.onDayClick(target, dayClickEvent.getView(), dayClickEvent.getDate(), dayClickEvent.isAllDay());
		}

		else if (event instanceof ClickEvent)
		{
			ClickEvent clickEvent = (ClickEvent) event;
			this.onEventClick(target, clickEvent.getView(), clickEvent.getEventId());
		}

		else if (event instanceof DropEvent)
		{
			DropEvent dropEvent = (DropEvent) event;
			this.onEventDrop(target, dropEvent.getEventId(), dropEvent.getDelta(), dropEvent.isAllDay());
		}

		else if (event instanceof ResizeEvent)
		{
			ResizeEvent resizeEvent = (ResizeEvent) event;
			this.onEventResize(target, resizeEvent.getEventId(), resizeEvent.getDelta());
		}

		else if (event instanceof ObjectDropEvent)
		{
			ObjectDropEvent dropEvent = (ObjectDropEvent) event;
			this.onObjectDrop(target, dropEvent.getTitle(), dropEvent.getDate(), dropEvent.isAllDay());
		}

		else if (event instanceof ViewRenderEvent)
		{
			ViewRenderEvent renderEvent = (ViewRenderEvent) event;
			this.onViewRender(target, renderEvent.getView(), renderEvent.getStart(), renderEvent.getEnd());
		}
	}

	// Factories //

	/**
	 * Gets the ajax behavior that will be triggered when the user select a cell range
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnSelectAjaxBehavior(IJQueryAjaxAware source)
	{
		return new OnSelectAjaxBehavior(source);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user clicks on a day cell
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnDayClickAjaxBehavior(IJQueryAjaxAware source)
	{
		return new OnDayClickAjaxBehavior(source);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user clicks on an event
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnEventClickAjaxBehavior(IJQueryAjaxAware source)
	{
		return new OnEventClickAjaxBehavior(source);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user moves (drag & drop) an event
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnEventDropAjaxBehavior(IJQueryAjaxAware source, CharSequence precondition)
	{
		return new OnEventDropAjaxBehavior(source, precondition);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user resizes an event
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnEventResizeAjaxBehavior(IJQueryAjaxAware source, CharSequence precondition)
	{
		return new OnEventResizeAjaxBehavior(source, precondition);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user drops an event object
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnObjectDropAjaxBehavior(IJQueryAjaxAware source)
	{
		return new OnObjectDropAjaxBehavior(source);
	}

	/**
	 * Gets the ajax behavior that will be triggered when the user changes the view, or when any of the date navigation methods are called.
	 *
	 * @return the {@link JQueryAjaxBehavior}
	 */
	protected JQueryAjaxBehavior newOnViewRenderAjaxBehavior(IJQueryAjaxAware source)
	{
		return new OnViewRenderAjaxBehavior(source);
	}

	// Ajax classes //

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnSelectAjaxBehavior(IJQueryAjaxAware)}
	 */
	protected static class OnSelectAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public OnSelectAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://fullcalendar.io/docs/selection/select_callback/
			// function(startDate, endDate, jsEvent, view) { }
			return new CallbackParameter[] { CallbackParameter.converted("startDate", "startDate.format()"), // retrieved
					CallbackParameter.converted("endDate", "endDate.format()"), // retrieved
					CallbackParameter.resolved("allDay", "!startDate.hasTime()"), // retrieved
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("view"), // lf
					CallbackParameter.resolved("viewName", "view.name") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new SelectEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnDayClickAjaxBehavior(IJQueryAjaxAware)}
	 */
	protected static class OnDayClickAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public OnDayClickAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://fullcalendar.io/docs/mouse/dayClick/
			// function(date, allDay, jsEvent, view)
			return new CallbackParameter[] { CallbackParameter.converted("date", "date.format()"), // retrieved
					CallbackParameter.resolved("allDay", "!date.hasTime()"), // retrieved
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("view"),// lf
					CallbackParameter.resolved("viewName", "view.name") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new DayClickEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnEventClickAjaxBehavior(IJQueryAjaxAware)}
	 */
	protected static class OnEventClickAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public OnEventClickAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://arshaw.com/fullcalendar/docs/mouse/eventClick/
			// function(event, jsEvent, view) { }
			return new CallbackParameter[] { CallbackParameter.context("event"), // lf
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("view"), // lf
					CallbackParameter.resolved("eventId", "event.id"),// retrieved
					CallbackParameter.resolved("viewName", "view.name") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new ClickEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnEventDropAjaxBehavior(IJQueryAjaxAware, CharSequence)}
	 */
	protected static class OnEventDropAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		private final CharSequence precondition;

		public OnEventDropAjaxBehavior(IJQueryAjaxAware source, CharSequence precondition)
		{
			super(source);

			this.precondition = precondition;
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);

			if (!Strings.isEmpty(this.precondition))
			{
				AjaxCallListener ajaxCallListener = new AjaxCallListener();
				ajaxCallListener.onPrecondition(this.precondition);

				attributes.getAjaxCallListeners().add(ajaxCallListener);
			}
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://fullcalendar.io/docs/event_ui/eventDrop/
			// function(event, delta, revertFunc, jsEvent, ui, view) { }
			return new CallbackParameter[] { CallbackParameter.context("event"), // lf
					CallbackParameter.context("delta"), // lf
					CallbackParameter.resolved("millisDelta", "delta.asMilliseconds()"), // retrieved
					CallbackParameter.resolved("allDay", "!event.start.hasTime()"), // retrieved
					CallbackParameter.context("revertFunc"), // lf
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("ui"), // lf
					CallbackParameter.context("view"), // lf
					CallbackParameter.resolved("eventId", "event.id") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new DropEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnEventResizeAjaxBehavior(IJQueryAjaxAware, CharSequence)}
	 */
	protected static class OnEventResizeAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		private final CharSequence precondition;

		public OnEventResizeAjaxBehavior(IJQueryAjaxAware source, CharSequence precondition)
		{
			super(source);

			this.precondition = precondition;
		}

		@Override
		protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
		{
			super.updateAjaxAttributes(attributes);

			if (!Strings.isEmpty(this.precondition))
			{
				AjaxCallListener ajaxCallListener = new AjaxCallListener();
				ajaxCallListener.onPrecondition(this.precondition);

				attributes.getAjaxCallListeners().add(ajaxCallListener);
			}
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://fullcalendar.io/docs/event_ui/eventResize/
			// function(event, delta, revertFunc, jsEvent, ui, view) { }
			return new CallbackParameter[] { CallbackParameter.context("event"), // lf
					CallbackParameter.context("delta"), // lf
					CallbackParameter.context("revertFunc"), // lf
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("ui"), // lf
					CallbackParameter.context("view"), // lf
					CallbackParameter.resolved("millisDelta", "delta.asMilliseconds()"), // retrieved
					CallbackParameter.resolved("allDay", "!event.start.hasTime()"), // retrieved
					CallbackParameter.resolved("eventId", "event.id") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new ResizeEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnObjectDropAjaxBehavior(IJQueryAjaxAware)}
	 */
	protected static class OnObjectDropAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public OnObjectDropAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://fullcalendar.io/docs/dropping/drop/
			// function(date, jsEvent, ui) { }
			return new CallbackParameter[] { CallbackParameter.converted("date", "date.format()"), // retrieved
					CallbackParameter.resolved("allDay", "!date.hasTime()"), // retrieved
					CallbackParameter.context("jsEvent"), // lf
					CallbackParameter.context("ui"), // lf
					CallbackParameter.resolved("title", "jQuery(this).data('title')") // retrieved
			};
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new ObjectDropEvent();
		}
	}

	/**
	 * Default {@link JQueryAjaxBehavior} implementation for {@link CalendarBehavior#newOnViewRenderAjaxBehavior(IJQueryAjaxAware)}
	 */
	protected static class OnViewRenderAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public OnViewRenderAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			// http://arshaw.com/fullcalendar/docs/display/viewRender/
			// function(view, element) { }
			return new CallbackParameter[] { CallbackParameter.context("view"),// lf
					CallbackParameter.context("element"), // lf
					CallbackParameter.resolved("viewName", "view.name"), // retrieved
					CallbackParameter.resolved("startDate", "view.start.format()"), // retrieved
					CallbackParameter.resolved("endDate", "view.end.format()") }; // retrieved
		}

		@Override
		protected JQueryEvent newEvent()
		{
			return new ViewRenderEvent();
		}
	}

	// Event objects //

	/**
	 * An event object that will be broadcasted when the user select a cell range
	 */
	protected static class SelectEvent extends JQueryEvent
	{
		private final LocalDateTime start;
		private final LocalDateTime end;
		private final boolean isAllDay;
		private final String viewName;

		public SelectEvent()
		{
			this.isAllDay = RequestCycleUtils.getQueryParameterValue("allDay").toBoolean();

			String start = RequestCycleUtils.getQueryParameterValue("startDate").toString();
			this.start = this.isAllDay ? LocalDate.parse(start).atStartOfDay() : LocalDateTime.parse(start);

			String end = RequestCycleUtils.getQueryParameterValue("endDate").toString();
			this.end = this.isAllDay ? LocalDate.parse(end).atStartOfDay() : LocalDateTime.parse(end);

			this.viewName = RequestCycleUtils.getQueryParameterValue("viewName").toString();
		}

		/**
		 * Gets the event start date
		 *
		 * @return the start date
		 */
		public LocalDateTime getStart()
		{
			return this.start;
		}

		/**
		 * Gets the end date
		 *
		 * @return the end date
		 */
		public LocalDateTime getEnd()
		{
			return this.end;
		}

		/**
		 * Indicates whether this event is an 'all-day' event
		 *
		 * @return true or false
		 */
		public boolean isAllDay()
		{
			return this.isAllDay;
		}

		/**
		 * Gets the current {@link CalendarView}
		 *
		 * @return the view name
		 */
		public CalendarView getView()
		{
			return CalendarView.get(this.viewName);
		}
	}

	/**
	 * An event object that will be broadcasted when the user clicks on a day cell
	 */
	protected static class DayClickEvent extends JQueryEvent
	{
		private final LocalDateTime day;
		private final boolean isAllDay;
		private final String viewName;

		/**
		 * Constructor
		 */
		public DayClickEvent()
		{
			this.isAllDay = RequestCycleUtils.getQueryParameterValue("allDay").toBoolean();

			String date = RequestCycleUtils.getQueryParameterValue("date").toString();
			this.day = this.isAllDay ? LocalDate.parse(date).atStartOfDay() : LocalDateTime.parse(date);

			this.viewName = RequestCycleUtils.getQueryParameterValue("viewName").toString();
		}

		/**
		 * Gets the event date
		 *
		 * @return the date
		 */
		public LocalDateTime getDate()
		{
			return this.day;
		}

		/**
		 * Indicates whether this event is an 'all-day' event
		 *
		 * @return true or false
		 */
		public boolean isAllDay()
		{
			return this.isAllDay;
		}

		/**
		 * Gets the current {@link CalendarView}
		 *
		 * @return the view name
		 */
		public CalendarView getView()
		{
			return CalendarView.get(this.viewName);
		}
	}

	/**
	 * An event object that will be broadcasted when the user clicks on an event
	 */
	protected static class ClickEvent extends JQueryEvent
	{
		private final int eventId;
		private final String viewName;

		/**
		 * Constructor
		 */
		public ClickEvent()
		{
			this.eventId = RequestCycleUtils.getQueryParameterValue("eventId").toInt();
			this.viewName = RequestCycleUtils.getQueryParameterValue("viewName").toString();
		}

		/**
		 * Gets the event's id
		 *
		 * @return the event's id
		 */
		public int getEventId()
		{
			return this.eventId;
		}

		/**
		 * Gets the current {@link CalendarView}
		 *
		 * @return the view name
		 */
		public CalendarView getView()
		{
			return CalendarView.get(this.viewName);
		}
	}

	/**
	 * An event object that will be broadcasted when the calendar loads and every time a different date-range is displayed.
	 */
	protected static class ViewRenderEvent extends JQueryEvent
	{
		private final LocalDateTime start;
		private final LocalDateTime end;
		private final String viewName;

		/**
		 * Constructor
		 */
		public ViewRenderEvent()
		{
			String start = RequestCycleUtils.getQueryParameterValue("startDate").toString();
			this.start = LocalDateTime.parse(start);

			String end = RequestCycleUtils.getQueryParameterValue("endDate").toString();
			this.end = LocalDateTime.parse(end);

			this.viewName = RequestCycleUtils.getQueryParameterValue("viewName").toString();
		}

		/**
		 * Gets the event start date
		 *
		 * @return the start date
		 */
		public LocalDateTime getStart()
		{
			return this.start;
		}

		/**
		 * Gets the end date
		 *
		 * @return the end date
		 */
		public LocalDateTime getEnd()
		{
			return this.end;
		}

		/**
		 * Gets the current {@link CalendarView}
		 *
		 * @return the view name
		 */
		public CalendarView getView()
		{
			return CalendarView.get(this.viewName);
		}
	}

	/**
	 * A base event object that contains a delta time
	 */
	protected abstract static class DeltaEvent extends JQueryEvent
	{
		private final int eventId;
		private long delta;

		/**
		 * Constructor
		 */
		public DeltaEvent()
		{
			this.eventId = RequestCycleUtils.getQueryParameterValue("eventId").toInt();

			this.delta = RequestCycleUtils.getQueryParameterValue("millisDelta").toLong();
		}

		/**
		 * Gets the event's id
		 *
		 * @return the event's id
		 */
		public int getEventId()
		{
			return this.eventId;
		}

		/**
		 * Gets the event's delta time in milliseconds
		 *
		 * @return the event's delta time
		 */
		public long getDelta()
		{
			return this.delta;
		}
	}

	/**
	 * An event object that will be broadcasted when the user moves (drag & drop) an event
	 */
	protected static class DropEvent extends DeltaEvent
	{
		private final boolean isAllDay;

		/**
		 * Constructor
		 */
		public DropEvent()
		{
			this.isAllDay = RequestCycleUtils.getQueryParameterValue("allDay").toBoolean();
		}

		/**
		 * Indicates whether this event is an 'all-day' event
		 *
		 * @return true or false
		 */
		public boolean isAllDay()
		{
			return this.isAllDay;
		}
	}

	/**
	 * An event object that will be broadcasted when the user resizes an event
	 */
	protected static class ResizeEvent extends DeltaEvent
	{
	}

	/**
	 * An event object that will be broadcasted when the user drops an event-object
	 */
	protected static class ObjectDropEvent extends JQueryEvent
	{
		private final LocalDateTime day;
		private final String title;
		private final boolean isAllDay;

		/**
		 * Constructor
		 */
		public ObjectDropEvent()
		{
			this.isAllDay = RequestCycleUtils.getQueryParameterValue("allDay").toBoolean();

			String date = RequestCycleUtils.getQueryParameterValue("date").toString();
			this.day = this.isAllDay ? LocalDate.parse(date).atStartOfDay() : LocalDateTime.parse(date);

			this.title = RequestCycleUtils.getQueryParameterValue("title").toString();
		}

		/**
		 * Gets the event date
		 *
		 * @return the date
		 */
		public LocalDateTime getDate()
		{
			return this.day;
		}

		/**
		 * Gets the event title
		 *
		 * @return the title
		 */
		public String getTitle()
		{
			return this.title;
		}

		/**
		 * Indicates whether this event is an 'all-day' event
		 *
		 * @return true or false
		 */
		public boolean isAllDay()
		{
			return this.isAllDay;
		}
	}
}
