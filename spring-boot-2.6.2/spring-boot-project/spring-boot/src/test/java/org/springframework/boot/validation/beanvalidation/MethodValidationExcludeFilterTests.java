/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.validation.beanvalidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodValidationExcludeFilter}.
 *
 * @author Andy Wilkinson
 */
class MethodValidationExcludeFilterTests {

	@Test
	void byAnnotationWhenClassIsAnnotatedExcludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
		assertThat(filter.isExcluded(Annotated.class)).isTrue();
	}

	@Test
	void byAnnotationWhenClassIsNotAnnotatedIncludes() {
		MethodValidationExcludeFilter filter = MethodValidationExcludeFilter.byAnnotation(Indicator.class);
		assertThat(filter.isExcluded(Plain.class)).isFalse();
	}

	static class Plain {

	}

	@Indicator
	static class Annotated {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator {

	}

}
