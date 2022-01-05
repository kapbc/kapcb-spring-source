/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Dynatrace
 * metrics export.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.dynatrace")
public class DynatraceProperties extends StepRegistryProperties {

	private final V1 v1 = new V1();

	private final V2 v2 = new V2();

	/**
	 * Dynatrace authentication token.
	 */
	private String apiToken;

	/**
	 * URI to ship metrics to. Should be used for SaaS, self managed instances or to
	 * en-route through an internal proxy.
	 */
	private String uri;

	public String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "management.metrics.export.dynatrace.v1.device-id")
	public String getDeviceId() {
		return this.v1.getDeviceId();
	}

	@Deprecated
	public void setDeviceId(String deviceId) {
		this.v1.setDeviceId(deviceId);
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "management.metrics.export.dynatrace.v1.technology-type")
	public String getTechnologyType() {
		return this.v1.getTechnologyType();
	}

	@Deprecated
	public void setTechnologyType(String technologyType) {
		this.v1.setTechnologyType(technologyType);
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "management.metrics.export.dynatrace.v1.group")
	public String getGroup() {
		return this.v1.getGroup();
	}

	@Deprecated
	public void setGroup(String group) {
		this.v1.setGroup(group);
	}

	public V1 getV1() {
		return this.v1;
	}

	public V2 getV2() {
		return this.v2;
	}

	public static class V1 {

		/**
		 * ID of the custom device that is exporting metrics to Dynatrace.
		 */
		private String deviceId;

		/**
		 * Group for exported metrics. Used to specify custom device group name in the
		 * Dynatrace UI.
		 */
		private String group;

		/**
		 * Technology type for exported metrics. Used to group metrics under a logical
		 * technology name in the Dynatrace UI.
		 */
		private String technologyType = "java";

		public String getDeviceId() {
			return this.deviceId;
		}

		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}

		public String getGroup() {
			return this.group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public String getTechnologyType() {
			return this.technologyType;
		}

		public void setTechnologyType(String technologyType) {
			this.technologyType = technologyType;
		}

	}

	public static class V2 {

		/**
		 * Default dimensions that are added to all metrics in the form of key-value
		 * pairs. These are overwritten by Micrometer tags if they use the same key.
		 */
		private Map<String, String> defaultDimensions;

		/**
		 * Whether to enable Dynatrace metadata export.
		 */
		private boolean enrichWithDynatraceMetadata = true;

		/**
		 * Prefix string that is added to all exported metrics.
		 */
		private String metricKeyPrefix;

		public Map<String, String> getDefaultDimensions() {
			return this.defaultDimensions;
		}

		public void setDefaultDimensions(Map<String, String> defaultDimensions) {
			this.defaultDimensions = defaultDimensions;
		}

		public boolean isEnrichWithDynatraceMetadata() {
			return this.enrichWithDynatraceMetadata;
		}

		public void setEnrichWithDynatraceMetadata(Boolean enrichWithDynatraceMetadata) {
			this.enrichWithDynatraceMetadata = enrichWithDynatraceMetadata;
		}

		public String getMetricKeyPrefix() {
			return this.metricKeyPrefix;
		}

		public void setMetricKeyPrefix(String metricKeyPrefix) {
			this.metricKeyPrefix = metricKeyPrefix;
		}

	}

}
