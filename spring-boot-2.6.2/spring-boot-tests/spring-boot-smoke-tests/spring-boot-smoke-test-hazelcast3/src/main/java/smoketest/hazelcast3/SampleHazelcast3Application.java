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

package smoketest.hazelcast3;

import com.hazelcast.spring.cache.HazelcastCacheManager;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class SampleHazelcast3Application {

	public static void main(String[] args) {
		SpringApplication.run(SampleHazelcast3Application.class, args);
	}

	@Bean
	public ApplicationRunner registerCache(CountryRepository repository, HazelcastCacheManager cacheManager,
			CacheMetricsRegistrar registrar) {
		return (args) -> {
			repository.findByCode("BE");
			registrar.bindCacheToRegistry(cacheManager.getCache("countries"));
		};
	}

}
