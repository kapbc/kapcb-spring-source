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

package org.springframework.boot.logging;

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingSystem} when Logback is not on the classpath.
 *
 * @author Andy Wilkinson
 */
// Log4j2 is implicitly excluded due to LOG4J-2030
@ClassPathExclusions("logback-*.jar")
class LogbackAndLog4J2ExcludedLoggingSystemTests {

	@Test
	void whenLogbackAndLog4J2AreNotPresentJULIsTheLoggingSystem() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(JavaLoggingSystem.class);
	}

}
