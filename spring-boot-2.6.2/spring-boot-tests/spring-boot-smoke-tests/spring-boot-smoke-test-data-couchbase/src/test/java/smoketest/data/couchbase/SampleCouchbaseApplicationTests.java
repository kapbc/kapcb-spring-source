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

package smoketest.data.couchbase;

import com.couchbase.client.core.error.AmbiguousTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.NestedCheckedException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SampleCouchbaseApplicationTests {

	@Test
	void testDefaultSettings(CapturedOutput output) {
		try {
			new SpringApplicationBuilder(SampleCouchbaseApplication.class).run("--server.port=0");
		}
		catch (RuntimeException ex) {
			if (serverNotRunning(ex)) {
				return;
			}
		}
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	private boolean serverNotRunning(RuntimeException ex) {
		NestedCheckedException nested = new NestedCheckedException("failed", ex) {
		};
		if (nested.contains(AmbiguousTimeoutException.class)) {
			Throwable root = nested.getRootCause();
			// This is not ideal, we should have a better way to know what is going on
			if (root.getMessage().contains("QueryRequest, Reason: TIMEOUT")) {
				return true;
			}
		}
		return false;
	}

}
