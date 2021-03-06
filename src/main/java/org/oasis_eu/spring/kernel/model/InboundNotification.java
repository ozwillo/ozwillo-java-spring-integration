package org.oasis_eu.spring.kernel.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.oasis_eu.spring.kernel.model.util.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Locale;


public class InboundNotification {
	private static final Logger logger = LoggerFactory.getLogger(InboundNotification.class);

	@JsonProperty("id")
	private String id;

	@JsonProperty("user_id")
	private String userId;

	@JsonProperty("instance_id")
	private String instanceId;

	@JsonProperty("message")
	private String message;
	private LocalizedString localizedmessages;

	@JsonProperty("service_id")
	private String serviceId;

	@JsonProperty("action_uri")
	private String actionUri;

	@JsonProperty("action_label")
	private String actionLabel;
	private LocalizedString localizedActionLabels;

	@JsonProperty("status")
	private NotificationStatus status;

	@JsonProperty("time")
	private Instant time;

	public  InboundNotification(){
		localizedmessages = new LocalizedString();
		localizedActionLabels = new LocalizedString();
	}

	@JsonAnySetter
	public void anySetter(String key, String value) {
		if (key.startsWith("message#")) {
			localizedmessages.setLocalizedString(key.substring("message#".length()), value);
		} else if (key.startsWith("action_label#")) {
			localizedActionLabels.setLocalizedString(key.substring("action_label#".length()), value);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage(Locale locale) {
		String localMessage = localizedmessages.getLocalizedString(locale);
		return localMessage !=null ? localMessage : message;
	}

	@JsonIgnore
	public void setLocalizedMessage(LocalizedString localizedmessages) {
		this.localizedmessages = localizedmessages;
	}

	public String getActionUri() {
		return actionUri;
	}

	public void setActionUri(String actionUri) {
		this.actionUri = actionUri;
	}

	public String getActionLabel() {
		return actionLabel;
	}

	public String getActionLabel(Locale locale) {
		String localActionLabels = localizedActionLabels.getLocalizedString(locale);
		return localActionLabels !=null ? localActionLabels : actionLabel;
	}

	public void setActionLabel(String actionLabel) {
		this.actionLabel = actionLabel;
	}

	@JsonIgnore
	public void setLocalizedActionLabels(LocalizedString localizedactionLabels) {
		this.localizedActionLabels = localizedactionLabels;
	}

	public NotificationStatus getStatus() {
		return status;
	}

	public void setStatus(NotificationStatus status) {
		this.status = status;
	}

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}
}
